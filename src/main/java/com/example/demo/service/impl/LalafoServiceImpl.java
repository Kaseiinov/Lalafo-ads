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

            List<Ad> pageAds = parsePage(doc);
            if (pageAds.isEmpty()) {
                log.warn("Страница №{} не содержит объявлений, останавливаемся", page);
                break;
            }

            ads.addAll(pageAds);
            page++;
            pause();

        }

        log.info("Загрузка завершена. Итого: {} объявлений", ads.size());
        return ads.size() > MAX_ADS ? ads.subList(0, MAX_ADS) : ads;
    }

    private List<Ad> parsePage(Document doc) {
        List<Ad> ads = new ArrayList<>();

        Elements titleElements = doc.select("p[class*=LFSubHeading][class*=weight-700]");
        log.info("Найдено заголовков на странице: {}", titleElements.size());

        for (Element titleElement : titleElements) {
            Ad ad = buildAd(titleElement);
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



    private Ad buildAd(Element titleElement) {
        String title = titleElement.text().trim();
        if (title.isEmpty()) {
            return null;
        }

        Element card = findParentCard(titleElement);

        return Ad.builder()
                .title(title)
                .price(parsePrice(titleElement))
                .city(parseCity(card))
                .date(parseDate(card))
                .imageUrl(parseImage(card))
                .adUrl(parseUrl(card))
                .build();
    }

    private String parsePrice(Element titleElement) {
        Element nextElement = titleElement.nextElementSibling();
        if (nextElement != null && !nextElement.text().trim().isEmpty()) {
            return nextElement.text().trim();
        }
        return "Договорная";
    }

    private String parseCity(Element card) {
        if (card == null) return "Кыргызстан";

        Element cityElement = card.selectFirst(
                "[class*=city], [class*=City], [class*=location], [class*=Location]"
        );

        if (cityElement != null && !cityElement.text().trim().isEmpty()) {
            return cityElement.text().trim();
        }

        return "Кыргызстан";
    }


    private String parseDate(Element card) {
        if (card == null) return "—";

        Element timeElement = card.selectFirst("time");
        if (timeElement != null) {
            String datetime = timeElement.attr("datetime");
            if (!datetime.isBlank()) return datetime;
            if (!timeElement.text().trim().isEmpty()) return timeElement.text().trim();
        }

        Element dateElement = card.selectFirst("[class*=date], [class*=Date], [class*=ago]");
        if (dateElement != null && !dateElement.text().trim().isEmpty()) {
            return dateElement.text().trim();
        }

        return "—";
    }

    private String parseImage(Element card) {
        if (card == null) return PLACEHOLDER_IMAGE;

        Element img = card.selectFirst("img");
        if (img == null) return PLACEHOLDER_IMAGE;

        String[] possibleAttributes = {"src", "data-src", "data-lazy-src", "data-original"};
        for (String attr : possibleAttributes) {
            String url = img.attr(attr);
            if (!url.isBlank() && url.startsWith("http")) {
                return url;
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
