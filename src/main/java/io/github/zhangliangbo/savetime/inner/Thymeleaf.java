package io.github.zhangliangbo.savetime.inner;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.zhangliangbo.savetime.ST;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.messageresolver.StandardMessageResolver;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

import java.util.Locale;
import java.util.Map;

/**
 * @author zhangliangbo
 * @since 2023/2/1
 */
public class Thymeleaf extends AbstractConfigurable<TemplateEngine> {

    @Override
    protected boolean isValid(TemplateEngine templateEngine) {
        return true;
    }

    @Override
    protected TemplateEngine create(String key) throws Exception {
        TemplateEngine templateEngine = new TemplateEngine();

        JsonNode treeNode = ST.io.readTree(getConfig());
        JsonNode thymeleaf = treeNode.get(key);
        String mode = thymeleaf.get("mode").asText();

        StringTemplateResolver stringTemplateResolver = new StringTemplateResolver();
        stringTemplateResolver.setCacheable(false);
        TemplateMode templateMode = TemplateMode.parse(mode);
        stringTemplateResolver.setTemplateMode(templateMode);
        templateEngine.addTemplateResolver(stringTemplateResolver);

        StandardMessageResolver standardMessageResolver = new StandardMessageResolver();
        templateEngine.addMessageResolver(standardMessageResolver);

        return templateEngine;
    }

    public String process(String key, Locale locale, String template, Map<String, Object> data) throws Exception {
        Context context = new Context();
        context.setVariables(data);
        context.setLocale(locale);
        return getOrCreate(key).process(template, context);
    }

    public String process(String key, String template, Map<String, Object> data) throws Exception {
        return process(key, Locale.getDefault(), template, data);
    }

}
