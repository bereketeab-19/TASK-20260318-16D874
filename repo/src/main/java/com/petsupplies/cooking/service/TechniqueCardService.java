package com.petsupplies.cooking.service;

import com.petsupplies.cooking.domain.TechniqueCard;
import com.petsupplies.cooking.domain.TechniqueTag;
import com.petsupplies.cooking.repo.TechniqueCardRepository;
import com.petsupplies.cooking.repo.TechniqueTagRepository;
import com.petsupplies.cooking.web.dto.CreateTechniqueCardRequest;
import com.petsupplies.cooking.web.dto.UpdateTechniqueCardRequest;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TechniqueCardService {
  private final TechniqueCardRepository techniqueCardRepository;
  private final TechniqueTagRepository techniqueTagRepository;
  private final Clock clock;

  public TechniqueCardService(
      TechniqueCardRepository techniqueCardRepository,
      TechniqueTagRepository techniqueTagRepository,
      Clock clock
  ) {
    this.techniqueCardRepository = techniqueCardRepository;
    this.techniqueTagRepository = techniqueTagRepository;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public TechniqueCard requireScoped(Long id, String merchantId) {
    return techniqueCardRepository.findByIdAndMerchantId(id, merchantId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));
  }

  @Transactional(readOnly = true)
  public Page<TechniqueCard> list(String merchantId, String tagFilter, Pageable pageable) {
    if (tagFilter != null && !tagFilter.isBlank()) {
      return techniqueCardRepository.findByMerchantIdAndTagName(merchantId, tagFilter.trim(), pageable);
    }
    return techniqueCardRepository.findByMerchantIdOrderByUpdatedAtDesc(merchantId, pageable);
  }

  /** Maps cards inside the transaction so tag associations load without OSIV. */
  @Transactional(readOnly = true)
  public Page<Map<String, Object>> listMaps(String merchantId, String tagFilter, Pageable pageable) {
    return list(merchantId, tagFilter, pageable).map(this::toMap);
  }

  @Transactional(readOnly = true)
  public Map<String, Object> toMap(Long id, String merchantId) {
    return toMap(requireScoped(id, merchantId));
  }

  private Map<String, Object> toMap(TechniqueCard c) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("id", c.getId());
    m.put("title", c.getTitle());
    m.put("body", c.getBody());
    m.put("createdAt", c.getCreatedAt());
    m.put("updatedAt", c.getUpdatedAt());
    m.put(
        "tags",
        c.getTags().stream().map(TechniqueTag::getName).sorted().collect(Collectors.toList())
    );
    return m;
  }

  @Transactional
  public Map<String, Object> create(String merchantId, CreateTechniqueCardRequest req) {
    Instant now = Instant.now(clock);
    TechniqueCard card = new TechniqueCard();
    card.setMerchantId(merchantId);
    card.setTitle(req.title().trim());
    card.setBody(req.body().trim());
    card.setCreatedAt(now);
    card.setUpdatedAt(now);
    card.setTags(resolveTags(merchantId, req.tags() == null ? List.of() : req.tags()));
    TechniqueCard saved = techniqueCardRepository.save(card);
    return toMap(saved.getId(), merchantId);
  }

  @Transactional
  public Map<String, Object> update(String merchantId, Long id, UpdateTechniqueCardRequest req) {
    TechniqueCard card = requireScoped(id, merchantId);
    if (req.title() != null && !req.title().isBlank()) {
      card.setTitle(req.title().trim());
    }
    if (req.body() != null && !req.body().isBlank()) {
      card.setBody(req.body().trim());
    }
    if (req.tags() != null) {
      card.getTags().clear();
      card.getTags().addAll(resolveTags(merchantId, req.tags()));
    }
    card.setUpdatedAt(Instant.now(clock));
    techniqueCardRepository.save(card);
    return toMap(id, merchantId);
  }

  @Transactional
  public void delete(String merchantId, Long id) {
    TechniqueCard card = requireScoped(id, merchantId);
    techniqueCardRepository.delete(card);
  }

  private Set<TechniqueTag> resolveTags(String merchantId, List<String> tagNames) {
    Set<TechniqueTag> out = new HashSet<>();
    List<String> normalized = tagNames.stream()
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .distinct()
        .collect(Collectors.toList());
    for (String raw : normalized) {
      TechniqueTag tag = techniqueTagRepository
          .findByMerchantIdAndNameIgnoreCase(merchantId, raw)
          .orElseGet(() -> {
            TechniqueTag t = new TechniqueTag();
            t.setMerchantId(merchantId);
            t.setName(raw);
            t.setCreatedAt(Instant.now(clock));
            return techniqueTagRepository.save(t);
          });
      out.add(tag);
    }
    return out;
  }
}
