package br.com.autevia.finkidsapi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI finKidsOpenApi() {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes(
                        "AutomationBearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("API Token")
                                .description("Token dedicado para chamadas de automacao (n8n/WhatsApp).")
                ).addSecuritySchemes(
                        "UserBearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT de usuario autenticado no fluxo Google OAuth do WebApp.")
                ))
                .info(new Info()
                        .title("Fin Kids API")
                        .version("v1")
                        .description("API central do projeto GranaGalaxy para contas, transacoes, metas e bonus.")
                        .contact(new Contact().name("Autevia").url("https://autevia.com.br")));
    }
}
