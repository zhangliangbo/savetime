package io.github.zhangliangbo.savetime.inner;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.generator.AutoGenerator;
import com.baomidou.mybatisplus.generator.config.DataSourceConfig;
import com.baomidou.mybatisplus.generator.config.GlobalConfig;
import com.baomidou.mybatisplus.generator.config.PackageConfig;
import com.baomidou.mybatisplus.generator.config.StrategyConfig;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.zhangliangbo.savetime.ST;

/**
 * @author zhangliangbo
 * @since 2022/12/23
 */
public class MybatisPlusGenerator extends AbstractConfigurable<AutoGenerator> {
    @Override
    protected boolean isValid(AutoGenerator autoGenerator) {
        return true;
    }

    @Override
    protected AutoGenerator create(String key) throws Exception {
        return new AutoGenerator();
    }

    public boolean generate(String env, String schema, String outputDir, String tablePrefix, String entitySuffix, String... table) throws Exception {
        String key = env + "-" + schema;
        JsonNode e = ST.jdbc.get(env, schema);
        AutoGenerator mpg = getOrCreate(key);

        GlobalConfig gc = new GlobalConfig();
        gc.setAuthor("someone");
        gc.setOutputDir(outputDir);
        gc.setFileOverride(true);
        gc.setOpen(false);
        gc.setBaseResultMap(true);
        gc.setIdType(IdType.AUTO);
        gc.setEntityName("%s" + entitySuffix);
        mpg.setGlobalConfig(gc);

        DataSourceConfig dsc = new DataSourceConfig();
        dsc.setDbType(DbType.MYSQL);
        dsc.setUrl(e.get("url").asText());
        dsc.setDriverName("com.mysql.jdbc.Driver");
        dsc.setUsername(e.get("username").asText());
        dsc.setPassword(e.get("password").asText());
        mpg.setDataSource(dsc);

        PackageConfig pc = new PackageConfig();
        pc.setParent("")
                .setEntity("")
                .setMapper("")
                .setXml("")
                .setService("")
                .setServiceImpl("")
                .setController("");
        mpg.setPackageInfo(pc);

        StrategyConfig strategy = new StrategyConfig();
        strategy.setTablePrefix(tablePrefix);
        strategy.setNaming(NamingStrategy.underline_to_camel);
        strategy.setColumnNaming(NamingStrategy.underline_to_camel);
        strategy.setEntityLombokModel(true);
        strategy.setRestControllerStyle(true);
        strategy.setInclude(table);
        mpg.setStrategy(strategy);
        mpg.execute();
        return true;
    }

}
