package net.daoke.openrestyplus.dictionary;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import java.sql.Types;
import java.util.UUID;

@Entity
@Table(name = "configuration_dictionary")
public class ConfigurationDictionary {
    @Id @JdbcTypeCode(Types.BINARY) private UUID id;
    @Column(name = "dictionary_type") private String dictionaryType;
    @Column(name = "dictionary_code") private String dictionaryCode;
    @Column(name = "dictionary_name") private String dictionaryName;
    @Column(name = "sort_order") private int sortOrder;
    private boolean enabled;
    protected ConfigurationDictionary() { }
    public ConfigurationDictionary(DictionaryType dictionaryType, String dictionaryCode, String dictionaryName,
                                   int sortOrder, boolean enabled) {
        this.id = UUID.randomUUID();
        apply(dictionaryType, dictionaryCode, dictionaryName, sortOrder, enabled);
    }
    public void apply(DictionaryType dictionaryType, String dictionaryCode, String dictionaryName,
                      int sortOrder, boolean enabled) {
        this.dictionaryType = dictionaryType.name();
        this.dictionaryCode = dictionaryCode;
        this.dictionaryName = dictionaryName;
        this.sortOrder = sortOrder;
        this.enabled = enabled;
    }
    public UUID getId() { return id; }
    public String getDictionaryType() { return dictionaryType; }
    public String getDictionaryCode() { return dictionaryCode; }
    public String getDictionaryName() { return dictionaryName; }
    public int getSortOrder() { return sortOrder; }
    public boolean isEnabled() { return enabled; }
}
