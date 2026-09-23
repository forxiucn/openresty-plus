package net.daoke.openrestyplus.dictionary;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ConfigurationDictionaryRepository extends JpaRepository<ConfigurationDictionary, UUID> {
    List<ConfigurationDictionary> findByDictionaryTypeAndEnabledTrueOrderBySortOrderAscDictionaryCodeAsc(String dictionaryType);
    boolean existsByDictionaryTypeAndDictionaryCode(String dictionaryType, String dictionaryCode);
}
