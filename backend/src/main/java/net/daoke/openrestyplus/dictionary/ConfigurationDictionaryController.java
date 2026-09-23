package net.daoke.openrestyplus.dictionary;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Common dictionary API for authenticated configuration forms. In the local profile
 * the whole control plane intentionally uses its local no-login security profile.
 */
@RestController
@RequestMapping("/api/dictionaries")
public class ConfigurationDictionaryController {
    private final ConfigurationDictionaryRepository dictionaries;

    public ConfigurationDictionaryController(ConfigurationDictionaryRepository dictionaries) {
        this.dictionaries = dictionaries;
    }

    /** Returns only enabled values, suitable for select controls. */
    @GetMapping("/{type}")
    public List<DictionaryView> list(@PathVariable DictionaryType type) {
        return dictionaries.findByDictionaryTypeAndEnabledTrueOrderBySortOrderAscDictionaryCodeAsc(type.name())
                .stream().map(DictionaryView::from).toList();
    }

    /** Management view keeps disabled options visible so an administrator can restore them. */
    @GetMapping("/{type}/all")
    public List<DictionaryView> listAll(@PathVariable DictionaryType type) {
        return dictionaries.findAll().stream()
                .filter(value -> type.name().equals(value.getDictionaryType()))
                .sorted((left, right) -> {
                    int sort = Integer.compare(left.getSortOrder(), right.getSortOrder());
                    return sort != 0 ? sort : left.getDictionaryCode().compareTo(right.getDictionaryCode());
                })
                .map(DictionaryView::from).toList();
    }

    @PostMapping("/{type}")
    @ResponseStatus(HttpStatus.CREATED)
    public DictionaryView create(@PathVariable DictionaryType type, @Valid @RequestBody DictionaryRequest request) {
        String code = normalizeCode(type, request.code());
        if (dictionaries.existsByDictionaryTypeAndDictionaryCode(type.name(), code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该字典值已存在");
        }
        try {
            return DictionaryView.from(dictionaries.save(new ConfigurationDictionary(type, code,
                    request.name().trim(), request.sortOrder(), request.enabled())));
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该字典值已存在");
        }
    }

    @PutMapping("/{type}/{dictionaryId}")
    public DictionaryView update(@PathVariable DictionaryType type, @PathVariable UUID dictionaryId,
                                 @Valid @RequestBody DictionaryRequest request) {
        ConfigurationDictionary value = requireValue(type, dictionaryId);
        String code = normalizeCode(type, request.code());
        boolean conflicts = dictionaries.existsByDictionaryTypeAndDictionaryCode(type.name(), code)
                && !code.equals(value.getDictionaryCode());
        if (conflicts) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该字典值已存在");
        }
        value.apply(type, code, request.name().trim(), request.sortOrder(), request.enabled());
        try {
            return DictionaryView.from(dictionaries.save(value));
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该字典值已存在");
        }
    }

    @DeleteMapping("/{type}/{dictionaryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable DictionaryType type, @PathVariable UUID dictionaryId) {
        dictionaries.delete(requireValue(type, dictionaryId));
    }

    private ConfigurationDictionary requireValue(DictionaryType type, UUID dictionaryId) {
        ConfigurationDictionary value = dictionaries.findById(dictionaryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "字典项不存在"));
        if (!type.name().equals(value.getDictionaryType())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "字典项不存在");
        }
        return value;
    }

    private String normalizeCode(DictionaryType type, String rawCode) {
        String code = rawCode.trim();
        if (type == DictionaryType.HTTP_METHOD) {
            code = code.toUpperCase(Locale.ROOT);
            if (!code.matches("[A-Z]{1,32}")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "HTTP 方法只能包含英文字母");
            }
            return code;
        }
        String normalized = code.toLowerCase(Locale.ROOT);
        if (!normalized.matches("(?:[a-z0-9!#$&^_.+-]+|\\*)/(?:[a-z0-9!#$&^_.+-]+|\\*)")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Content-Type 格式无效");
        }
        return normalized;
    }

    public record DictionaryRequest(
            @NotBlank @Size(max = 128) String code,
            @NotBlank @Size(max = 255) String name,
            @Min(0) @Max(100_000) int sortOrder,
            @NotNull Boolean enabled) { }

    public record DictionaryView(UUID id, String value, String label, int sortOrder, boolean enabled) {
        static DictionaryView from(ConfigurationDictionary value) {
            return new DictionaryView(value.getId(), value.getDictionaryCode(), value.getDictionaryName(),
                    value.getSortOrder(), value.isEnabled());
        }
    }
}
