package cams.config;


import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(buildInfo())
                .components(buildComponents())
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"));
    }

    private Info buildInfo() {
        return new Info()
                .title("CAMS - Career Application Management System")
                .version("1.0")
                .description("REST API for managing job applications. " +
                        "Register an account, log in to receive a JWT token, " +
                        "then use the Authorize button to authenticate.");
    }

    private Components buildComponents() {
        SecurityScheme bearerScheme = new SecurityScheme()
                .name("Bearer Authentication")
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT");

        return new Components().addSecuritySchemes("Bearer Authentication", bearerScheme);
    }
}
