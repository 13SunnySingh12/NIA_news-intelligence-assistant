package com.nia.config;

import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * {@link WebClient} for the internal FastAPI AI service. Every call carries the
 * shared {@code X-Internal-Token} header, which is never exposed to the browser.
 * A longer response timeout accommodates LLM latency.
 */
@Configuration
public class InternalClientConfig {

    public static final String FASTAPI_CLIENT = "fastapiWebClient";

    /**
     * Must exceed the AI service's own chat timeout plus one fallback attempt,
     * otherwise Spring cancels a request the AI service would have answered.
     */
    private static final int NIA_AI_RESPONSE_TIMEOUT_SECONDS = 60;

    @Bean(FASTAPI_CLIENT)
    public WebClient fastapiWebClient(NiaProperties props) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .responseTimeout(Duration.ofSeconds(NIA_AI_RESPONSE_TIMEOUT_SECONDS));

        return WebClient.builder()
                .baseUrl(props.getFastapiBaseUrl())
                .defaultHeader("X-Internal-Token", props.getInternalToken())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
