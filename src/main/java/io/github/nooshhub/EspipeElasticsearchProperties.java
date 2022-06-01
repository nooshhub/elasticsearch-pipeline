package io.github.nooshhub;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author neals
 * @since 5/31/2022
 */
@Component
@ConfigurationProperties(prefix = "espipe.elasticsearch")
public class EspipeElasticsearchProperties {
    private String host;
    private int port;
    private String protocol;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
}
