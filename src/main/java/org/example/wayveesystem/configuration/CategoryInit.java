package org.example.wayveesystem.configuration;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.wayveesystem.model.Category;
import org.example.wayveesystem.repository.CategoryRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Configuration
public class CategoryInit {

    @Bean
    ApplicationRunner categoryRunner(CategoryRepository categoryRepository) {
        return application -> {

            if (categoryRepository.findByCode("RESTAURANT").isEmpty()) {
                Category category = Category.builder()
                        .name("Restaurant")
                        .code("RESTAURANT")
                        .osmKey("amenity")
                        .osmValue("restaurant")
                        .description("Places that serve meals and food.")
                        .iconUrl("https://cdn-icons-png.flaticon.com/512/3075/3075977.png")
                        .build();

                categoryRepository.save(category);
                log.warn("Restaurant category has been created");
            }

            if (categoryRepository.findByCode("CAFE").isEmpty()) {
                Category category = Category.builder()
                        .name("Cafe")
                        .code("CAFE")
                        .osmKey("amenity")
                        .osmValue("cafe")
                        .description("Cafes and coffee shops for drinks and light meals.")
                        .iconUrl("https://cdn-icons-png.flaticon.com/512/924/924514.png")
                        .build();

                categoryRepository.save(category);
                log.warn("Cafe category has been created");
            }

            if (categoryRepository.findByCode("SHOPPING").isEmpty()) {
                Category category = Category.builder()
                        .name("Shopping")
                        .code("SHOPPING")
                        .osmKey("shop")
                        .osmValue("mall")
                        .description("Shopping malls and places for shopping.")
                        .iconUrl("https://cdn-icons-png.flaticon.com/512/3081/3081559.png")
                        .build();

                categoryRepository.save(category);
                log.warn("Shopping category has been created");
            }
        };
    }
}

