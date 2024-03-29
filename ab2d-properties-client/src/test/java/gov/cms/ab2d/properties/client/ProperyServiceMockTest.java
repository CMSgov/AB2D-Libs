package gov.cms.ab2d.properties.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import kong.unirest.json.JSONArray;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ProperyServiceMockTest {
    class PropertiesClientImplMock extends PropertiesClientImpl {
        @Override
        public String getFromEnvironment() {
            return "http://localhost:8065";
        }
    }

    @Test
    void testIt() {
        PropertiesClientImpl impl = new PropertiesClientImpl();
        int port = 8060;
        WireMockServer wireMockServer = new WireMockServer(port);
        wireMockServer.start();

        configureFor("localhost", port);

        List<Property> propertiesToReturn = List.of(new Property("a.key", "a.value"), new Property("b.key", "b.value"));
        JSONArray jsonArray = new JSONArray(propertiesToReturn);
        stubFor(get(urlEqualTo("/properties")).willReturn(aResponse().withBody(jsonArray.toString())));
        System.out.println(propertiesToReturn.get(0).toString());
        stubFor(get(urlEqualTo("/properties/a.key")).willReturn(aResponse().withBody("{ \"key\": \"a.key\", \"value\": \"a.value\"}")));
        stubFor(post(urlEqualTo("/properties"))
                .willReturn(aResponse().withBody("{ \"key\": \"one\", \"value\": \"two\"}")));
        stubFor(get(urlEqualTo("/properties/one")).willReturn(aResponse().withBody("{ \"key\": \"one\", \"value\": \"two\"}")));
        stubFor(get(urlEqualTo("/properties/bogus")).willReturn(aResponse().withStatus(404).withBody("{ \"key\": \"null\", \"value\": \"null\"}")));
        stubFor(delete(urlEqualTo("/properties/one")).willReturn(aResponse().withBody("true")));

        List<Property> properties = impl.getAllProperties();
        assertEquals(2, properties.size());

        Property retProperty = impl.getProperty("a.key");
        assertEquals("a.key", retProperty.getKey());
        assertEquals("a.value", retProperty.getValue());

        Property newProp = impl.setProperty("one", "two");
        assertEquals("one", newProp.getKey());
        assertEquals("two", newProp.getValue());

        assertThrows(PropertyNotFoundException.class, () -> impl.getProperty("bogus"));

        impl.deleteProperty("one");
        Property p = new Property();
        p.setKey("key");
        p.setValue("value");
        assertEquals("key", p.getKey());
        assertEquals("value", p.getValue());

        wireMockServer.stop();
    }

    @Test
    void testImpl() {
        PropertiesClientImplMock mock = new PropertiesClientImplMock();
        assertEquals("http://localhost:8065", mock.getUrl());
    }

    @Test
    void testErrors() {
        int port = 8065;
        PropertiesClientImpl impl = new PropertiesClientImpl("http://localhost:" + port);
        WireMockServer wireMockServer = new WireMockServer(port);
        wireMockServer.start();

        configureFor("localhost", port);

        stubFor(get(urlEqualTo("/properties")).willReturn(aResponse().withStatus(404)));
        stubFor(get(urlEqualTo("/properties/a.key")).willReturn(aResponse().withStatus(520)));
        stubFor(post(urlEqualTo("/properties")).willReturn((aResponse().withStatus(404))));
        stubFor(delete(urlEqualTo("/properties/one")).willReturn(aResponse().withBody("false")));
        //stubFor(post(urlEqualTo("/properties"))
         //       .willReturn(aResponse().withBody("{ \"key\": \"one\", \"value\": \"two\"}")));
        //stubFor(get(urlEqualTo("/properties/one")).willReturn(aResponse().withBody("{ \"key\": \"one\", \"value\": \"two\"}")));
        //stubFor(get(urlEqualTo("/properties/bogus")).willReturn(aResponse().withStatus(404).withBody("{ \"key\": \"null\", \"value\": \"null\"}")));
        //stubFor(delete(urlEqualTo("/properties/one")).willReturn(aResponse().withBody("true")));

        assertThrows(PropertyNotFoundException.class, () -> impl.getAllProperties());

        assertThrows(PropertyNotFoundException.class, () -> impl.getProperty("a.key"));

        Property newProp = impl.setProperty("one", "two");
        assertNull(newProp);

        assertThrows(PropertyNotFoundException.class, () -> impl.deleteProperty("one"));

        wireMockServer.stop();
    }
}
