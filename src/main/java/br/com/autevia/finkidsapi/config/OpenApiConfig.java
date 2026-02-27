package br.com.autevia.finkidsapi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI finKidsOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Fin Kids API")
                        .version("v1")
                        .description("API central do projeto GranaGalaxy para contas, transacoes, metas e bonus.")
                        .contact(new Contact().name("Autevia").url("https://autevia.com.br")));
    }
}
