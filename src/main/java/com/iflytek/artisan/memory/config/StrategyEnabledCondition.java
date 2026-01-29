package com.iflytek.artisan.memory.config;

import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.List;
import java.util.Map;

/**
 * Condition to check if a compression strategy is enabled in configuration.
 */
public class StrategyEnabledCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Map<String, Object> attributes = metadata.getAnnotationAttributes(
                ConditionalOnStrategyEnabled.class.getName());
        if (attributes == null) {
            return false;
        }
        String strategyName = (String) attributes.get("value");
        
        // Use Binder API to get list property from configuration
        Binder binder = Binder.get(context.getEnvironment());
        List<String> strategies = binder.bind("artisan.memory.compression.strategies", 
                Bindable.listOf(String.class))
                .orElse(List.of());
        
        if (strategies.isEmpty()) {
            return false;
        }
        
        return strategies.contains(strategyName);
    }
}
