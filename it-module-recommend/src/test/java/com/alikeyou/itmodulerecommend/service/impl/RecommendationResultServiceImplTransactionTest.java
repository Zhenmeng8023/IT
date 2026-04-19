package com.alikeyou.itmodulerecommend.service.impl;

import com.alikeyou.itmodulerecommend.dto.BlogRecommendationSnapshot;
import com.alikeyou.itmodulerecommend.repository.RecommendationResultRepository;
import com.alikeyou.itmodulerecommend.service.RecommendationResultService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringJUnitConfig(RecommendationResultServiceImplTransactionTest.TestConfig.class)
class RecommendationResultServiceImplTransactionTest {

    @Resource
    private OuterReadOnlyCaller outerReadOnlyCaller;

    @Resource
    private RecordingTransactionManager transactionManager;

    @Test
    void shouldOpenIndependentWritableTransactionInsideReadOnlyCaller() {
        transactionManager.clear();

        BlogRecommendationSnapshot snapshot = outerReadOnlyCaller.invoke();

        assertNotNull(snapshot);
        assertEquals(2, transactionManager.definitions().size());

        TransactionDefinition outerDefinition = transactionManager.definitions().get(0);
        assertTrue(outerDefinition.isReadOnly());
        assertEquals(TransactionDefinition.PROPAGATION_REQUIRED, outerDefinition.getPropagationBehavior());

        TransactionDefinition innerDefinition = transactionManager.definitions().get(1);
        assertFalse(innerDefinition.isReadOnly());
        assertEquals(TransactionDefinition.PROPAGATION_REQUIRES_NEW, innerDefinition.getPropagationBehavior());
    }

    @Configuration
    @EnableTransactionManagement(proxyTargetClass = true)
    static class TestConfig {

        @Bean
        RecordingTransactionManager transactionManager() {
            return new RecordingTransactionManager();
        }

        @Bean
        RecommendationResultService recommendationResultService() {
            return new RecommendationResultServiceImpl();
        }

        @Bean
        RecommendationResultRepository recommendationResultRepository() {
            return proxyFor(RecommendationResultRepository.class);
        }

        @Bean
        NamedParameterJdbcTemplate namedParameterJdbcTemplate() {
            return new NamedParameterJdbcTemplate(new AbstractDataSource() {
                @Override
                public Connection getConnection() throws SQLException {
                    throw new UnsupportedOperationException("not used in transaction propagation test");
                }

                @Override
                public Connection getConnection(String username, String password) throws SQLException {
                    throw new UnsupportedOperationException("not used in transaction propagation test");
                }
            });
        }

        @Bean
        EntityManager entityManager() {
            return proxyFor(EntityManager.class);
        }

        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        OuterReadOnlyCaller outerReadOnlyCaller(RecommendationResultService recommendationResultService) {
            return new OuterReadOnlyCaller(recommendationResultService);
        }

        @SuppressWarnings("unchecked")
        private static <T> T proxyFor(Class<T> type) {
            return (T) Proxy.newProxyInstance(
                    type.getClassLoader(),
                    new Class<?>[]{type},
                    (proxy, method, args) -> {
                        if (method.getDeclaringClass() == Object.class) {
                            return switch (method.getName()) {
                                case "hashCode" -> System.identityHashCode(proxy);
                                case "equals" -> proxy == args[0];
                                case "toString" -> type.getSimpleName() + "Stub";
                                default -> null;
                            };
                        }
                        if (!method.getReturnType().isPrimitive()) {
                            return null;
                        }
                        return switch (method.getReturnType().getName()) {
                            case "boolean" -> false;
                            case "byte" -> (byte) 0;
                            case "short" -> (short) 0;
                            case "int" -> 0;
                            case "long" -> 0L;
                            case "float" -> 0f;
                            case "double" -> 0d;
                            case "char" -> '\0';
                            default -> null;
                        };
                    }
            );
        }
    }

    static class OuterReadOnlyCaller {
        private final RecommendationResultService recommendationResultService;

        OuterReadOnlyCaller(RecommendationResultService recommendationResultService) {
            this.recommendationResultService = recommendationResultService;
        }

        @Transactional(readOnly = true)
        BlogRecommendationSnapshot invoke() {
            return recommendationResultService.getLatestBlogRecommendations(null, 6);
        }
    }

    static class RecordingTransactionManager implements PlatformTransactionManager {
        private final List<TransactionDefinition> definitions = new ArrayList<>();

        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            definitions.add(new DefaultTransactionDefinition(definition));
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
        }

        @Override
        public void rollback(TransactionStatus status) {
        }

        void clear() {
            definitions.clear();
        }

        List<TransactionDefinition> definitions() {
            return definitions;
        }
    }
}
