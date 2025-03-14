package com.rarchives.ripme.ripper.rippers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.rarchives.ripme.ripper.AbstractHTMLRipper;
import com.rarchives.ripme.utils.Http;

public class StaRipper extends AbstractHTMLRipper {

    private static final Logger logger = LogManager.getLogger(StaRipper.class);

    public StaRipper(URL url) throws IOException {
        super(url);
    }

    private Map<String,String> cookies = new HashMap<>();

    @Override
    public String getHost() {
        return "sta";
    }

    @Override
    public String getDomain() {
        return "sta.sh";
    }

    @Override
    public String getGID(URL url) throws MalformedURLException {
        Pattern p = Pattern.compile("https://sta.sh/([A-Za-z0-9]+)");
        Matcher m = p.matcher(url.toExternalForm());
        if (m.matches()) {
            return m.group(1);
        }
        throw new MalformedURLException("Expected sta.sh URL format: " +
                "sta.sh/ALBUMID - got " + url + " instead");
    }

    @Override
    public List<String> getURLsFromPage(Document doc) {
        List<String> result = new ArrayList<>();
        for (Element el : doc.select("span > span > a.thumb")) {
            String thumbPageURL = el.attr("href");
            Document thumbPage = null;
            if (checkURL(thumbPageURL)) {
                try {
                    Connection.Response resp = Http.url(new URI(thumbPageURL).toURL()).response();
                    cookies.putAll(resp.cookies());
                    thumbPage = resp.parse();
                } catch (MalformedURLException | URISyntaxException e) {
                    logger.info(thumbPageURL + " is a malformed URL");
                } catch (IOException e) {
                    logger.info(e.getMessage());
                }
                String imageDownloadUrl = thumbPage.select("a.dev-page-download").attr("href");
                if (imageDownloadUrl != null && !imageDownloadUrl.equals("")) {
                    result.add(getImageLinkFromDLLink(imageDownloadUrl));
                }
            }

        }
        return result;
    }

    private boolean checkURL(String url) {
        try {
            new URI(url).toURL();
            return true;
        } catch (MalformedURLException | URISyntaxException e) {
            return false;
        }
    }

    private String getImageLinkFromDLLink(String url) {
        try {
            Connection.Response response = Jsoup.connect(url)
                    .userAgent(USER_AGENT)
                    .timeout(10000)
                    .cookies(cookies)
                    .followRedirects(false)
                    .execute();
            String imageURL = response.header("Location");
            logger.info(imageURL);
            return imageURL;
            } catch (IOException e) {
                logger.info("Got error message " + e.getMessage() + " trying to download " + url);
                return null;
            }
    }

    @Override
    public void downloadURL(URL url, int index) {
        addURLToDownload(url, getPrefix(index));
    }
}
