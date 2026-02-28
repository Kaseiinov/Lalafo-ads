package com.example.demo.service.impl;

import com.example.demo.model.Ad;
import com.example.demo.service.LalafoService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


@Slf4j
@Service
public class LalafoServiceImpl implements LalafoService {

    private static final String BASE_URL = "https://lalafo.kg/kyrgyzstan/mobilnye-telefony-i-aksessuary/mobilnye-telefony";

    private static final String USER_AGENT =
            "Mozilla/5.0 (Linux; Android 10; SM-G975F) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                    "Chrome/120.0.0.0 Mobile Safari/537.36";

    private static final String PLACEHOLDER_IMAGE =
            "https://placehold.co/300x200/1a1a2e/ffffff?text=Фото";

    private static final int MAX_ADS   = 100;
    private static final int DELAY_MS  = 400;


    @Override
    public List<Ad> fetchAds() {
        List<Ad> ads = new ArrayList<>();
        int page = 1;

        while (ads.size() < MAX_ADS) {
            log.info("Загружаем страницу №{}, собрано: {}", page, ads.size());

            Document doc = loadPage(page);
            if (doc == null) break;

            int remaining = MAX_ADS - ads.size();
            List<Ad> pageAds = parsePage(doc, remaining);
            if (pageAds.isEmpty()) {
                log.warn("Страница №{} не содержит объявлений, останавливаемся", page);
                break;
            }

            ads.addAll(pageAds);
            page++;
            pause();

        }

        log.info("Загрузка завершена. Итого: {} объявлений", ads.size());
        log.info("Загрузка завершена. Итого: {} объявлений", ads);
        return ads;
    }

    private List<Ad> parsePage(Document doc, int remaining) {
        List<Ad> ads = new ArrayList<>();

        Elements priceElements = doc.select("p[style*='color:#0b78e3']");

        if (priceElements.isEmpty()) {
            priceElements = doc.select("p[class*=LFSubHeading][class*=weight-700]");
        }

        log.info("Найдено элементов на странице: {}", priceElements.size());

        for (Element priceElement : priceElements) {
            if (ads.size() >= remaining) break;

            Ad ad = buildAd(priceElement);
            if (ad != null) {
                ads.add(ad);
            }
        }

        return ads;
    }

    private void pause() {
        try {
            Thread.sleep(DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private Document loadPage(int page) {
        String url = BASE_URL + "?page=" + page;
        try {
            return Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .header("Accept-Language", "ru-RU,ru;q=0.9")
                    .timeout(20_000)
                    .get();
        } catch (Exception e) {
            log.error("Ошибка при загрузке страницы {}: {}", url, e.getMessage());
            return null;
        }
    }



    private Ad buildAd(Element priceElement) {
        String price = priceElement.text().trim();
        if (price.isEmpty()) return null;

        Element card = findParentCard(priceElement);

        Elements paragraphs = card != null ? card.select("p") : new Elements();

        String title = "Без названия";
        if (paragraphs.size() >= 3) {
            title = paragraphs.get(2).text().trim();
        }

        return Ad.builder()
                .title(title)
                .price(price)
                .city(parseCity(card))
                .date(parseDate(card))
                .imageUrl(parseImage(card))
                .adUrl(parseUrl(card))
                .build();
    }


    private String parseCity(Element card) {
        if (card == null) return "Кыргызстан";

        Element link = card.selectFirst("a[href]");
        if (link == null) return "Кыргызстан";

        String href = link.attr("href");
        if (href.startsWith("/")) {
            String[] parts = href.substring(1).split("/");
            if (parts.length >= 2 && parts[1].equals("ads")) {
                return slugToCity(parts[0]);
            }
        }
        return "Кыргызстан";
    }

    private String slugToCity(String slug) {
        return switch (slug) {
            case "bishkek"             -> "Бишкек";
            case "osh"                 -> "Ош";
            case "kant"                -> "Кант";
            case "tokmak"              -> "Токмак";
            case "karakol"             -> "Каракол";
            case "jalal-abad", "dzhalal-abad" -> "Джалал-Абад";
            case "naryn"               -> "Нарын";
            case "talas"               -> "Талас";
            case "kara-balta"          -> "Кара-Балта";
            case "nizhnyaya-ala-archa" -> "Нижняя Ала-Арча";
            case "ges-2"               -> "ГЭС-2";
            case "orto-saiy"           -> "Орто-Сай";
            case "manas"               -> "Манас";
            case "kok-dzhar"           -> "Кок-Жар";
            case "maevka"              -> "Маёвка";
            case "novopavlovka"        -> "Новопавловка";
            case "novopokrovka"        -> "Новопокровка";
            case "kyrgyzstan"          -> "Кыргызстан";
            default -> {
                if (slug.isEmpty()) yield "Кыргызстан";
                yield Character.toUpperCase(slug.charAt(0)) + slug.substring(1).replace("-", " ");
            }
        };
    }


    private String parseDate(Element card) {
        return java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }

    private String parseImage(Element card) {
        if (card == null) return PLACEHOLDER_IMAGE;

        Element img = card.selectFirst("img");
        if (img == null) return PLACEHOLDER_IMAGE;

        String[] attrs = {"data-src", "data-lazy-src", "data-original", "src"};
        for (String attr : attrs) {
            String url = img.attr(attr);
            if (url.isBlank()) continue;

            if (url.startsWith("http")) return url;
            if (url.startsWith("//")) return "https:" + url;

            if (url.contains("/_next/image") && url.contains("url=")) {
                try {
                    String encoded = url.substring(url.indexOf("url=") + 4);
                    int ampIndex = encoded.indexOf("&");
                    if (ampIndex != -1) encoded = encoded.substring(0, ampIndex);
                    return java.net.URLDecoder.decode(encoded, "UTF-8");
                } catch (Exception e) {
                    log.warn("Не удалось декодировать URL изображения: {}", url);
                }
            }
        }
        return PLACEHOLDER_IMAGE;
    }


        private String parseUrl(Element card) {
        if (card == null) return "https://lalafo.kg";

        Element link = card.selectFirst("a[href]");
        if (link == null) return "https://lalafo.kg";

        String href = link.attr("href");
        if (href.startsWith("http")) return href;
        if (href.startsWith("/"))    return "https://lalafo.kg" + href;

        return "https://lalafo.kg";
    }

    private Element findParentCard(Element element) {
        Element current = element.parent();

        for (int i = 0; i < 10; i++) {
            if (current == null) break;

            boolean hasImage = current.selectFirst("img") != null;
            boolean hasLink  = current.selectFirst("a[href]") != null;

            if (hasImage && hasLink) {
                return current;
            }

            current = current.parent();
        }

        return element.parent();
    }

}
