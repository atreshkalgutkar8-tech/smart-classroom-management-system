package com.smartclassroom.smart_classroom.service;

import com.smartclassroom.smart_classroom.entity.DIYKit;
import com.smartclassroom.smart_classroom.entity.DIYKitItem;
import com.smartclassroom.smart_classroom.repository.DIYKitItemRepository;
import com.smartclassroom.smart_classroom.repository.DIYKitRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DIYKitService {

    private final DIYKitRepository kitRepository;
    private final DIYKitItemRepository itemRepository;

    public DIYKitService(DIYKitRepository kitRepository, DIYKitItemRepository itemRepository) {
        this.kitRepository = kitRepository;
        this.itemRepository = itemRepository;
    }

    @Transactional(readOnly = true)
    public List<KitView> catalogue() {
        List<DIYKit> kits = kitRepository.findAllByOrderByNameAsc();
        if (kits.isEmpty()) return List.of();
        List<Long> kitIds = kits.stream().map(DIYKit::getId).toList();
        Map<Long, List<String>> itemsByKit = itemRepository.findByKit_IdInOrderByItemNameAsc(kitIds).stream()
                .collect(Collectors.groupingBy(item -> item.getKit().getId(), Collectors.mapping(
                        DIYKitItem::getItemName, Collectors.toList())));
        return kits.stream().map(kit -> new KitView(kit.getId(), kit.getName(), kit.getDescription(),
                itemsByKit.getOrDefault(kit.getId(), List.of()))).toList();
    }

    @Transactional
    public void create(String name, String description, String itemLines) {
        requireText(name, "Kit name", 180);
        requireText(description, "Kit description", 10000);
        requireText(itemLines, "Kit items", 10000);
        List<String> items = itemLines.lines().map(String::trim).filter(line -> !line.isEmpty()).toList();
        if (items.isEmpty()) throw new IllegalArgumentException("Add at least one item to the kit.");
        if (items.stream().anyMatch(item -> item.length() > 180)) {
            throw new IllegalArgumentException("Each kit item must be 180 characters or fewer.");
        }
        DIYKit kit = kitRepository.save(new DIYKit(name.trim(), description.trim()));
        itemRepository.saveAll(items.stream().map(item -> new DIYKitItem(kit, item)).toList());
    }

    private void requireText(String value, String label, int maxLength) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required.");
        if (value.trim().length() > maxLength) throw new IllegalArgumentException(label + " is too long.");
    }

    public record KitView(Long id, String name, String description, List<String> items) { }
}
