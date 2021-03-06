package org.embulk.parser.apache.log;


import org.embulk.config.ConfigSource;
import org.embulk.spi.ColumnConfig;
import org.embulk.spi.PageBuilder;
import org.embulk.spi.type.Type;

public abstract class LogElement<T> {

    protected String name;
    protected String regexp;
    protected final Type outputType;

    public LogElement(String name, String regex, Type outputType){
        this.name = name;
        this.regexp = regex;
        this.outputType = outputType;
    }

    public String getName(){
        return name;
    }

    public String getRegexp() {
        return regexp;
    }

    public Type getOutputType() {
        return outputType;
    }

    public abstract T parse(String s);

    public abstract void setToPageBuilder(PageBuilder pageBuilder, int i, String value);

    public ColumnConfig getColumnConfig(ConfigSource config){
        return new ColumnConfig(name, outputType, config);
    }

}
