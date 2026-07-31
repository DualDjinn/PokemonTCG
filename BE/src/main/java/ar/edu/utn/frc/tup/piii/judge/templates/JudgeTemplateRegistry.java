package ar.edu.utn.frc.tup.piii.judge.templates;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class JudgeTemplateRegistry {

    private static final String TEMPLATES_ES_PATH = "judge/templates_es.properties";
    private static final String TEMPLATES_EN_PATH = "judge/templates_en.properties";
    private static final String FALLBACK_PREFIX = "[TEMPLATE NOT FOUND] ";

    private Map<String, String> templatesEs;
    private Map<String, String> templatesEn;

    @PostConstruct
    public void init() {
        templatesEs = loadTemplates(TEMPLATES_ES_PATH);
        templatesEn = loadTemplates(TEMPLATES_EN_PATH);
    }

    public String resolve(String eventCode, String lang, Object... params) {
        Map<String, String> templates = "en".equals(lang) ? templatesEn : templatesEs;
        String template = templates.get(eventCode);
        if (template == null) {
            return FALLBACK_PREFIX + eventCode;
        }
        return applyParams(template, params);
    }

    public Set<String> registeredCodes(String lang) {
        Map<String, String> templates = "en".equals(lang) ? templatesEn : templatesEs;
        return Collections.unmodifiableSet(templates.keySet());
    }

    public boolean hasTemplate(String eventCode) {
        return templatesEs.containsKey(eventCode) && templatesEn.containsKey(eventCode);
    }

    private Map<String, String> loadTemplates(String classpathPath) {
        Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(classpathPath)) {
            if (is == null) {
                throw new IllegalStateException("Judge template file not found on classpath: "
                        + classpathPath);
            }
            props.load(new InputStreamReader(is, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load Judge template file: " + classpathPath, e);
        }
        Map<String, String> result = new HashMap<>();
        for (String key : props.stringPropertyNames()) {
            result.put(key, props.getProperty(key));
        }
        return Collections.unmodifiableMap(result);
    }

    private String applyParams(String template, Object... params) {
        if (params == null || params.length == 0) {
            return template;
        }
        String result = template;
        for (int i = 0; i < params.length; i++) {
            result = result.replace("{" + i + "}", String.valueOf(params[i]));
        }
        return result;
    }
}
