package com.smartclassroom.smart_classroom.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "diy_kit_items", indexes = @Index(name = "idx_diy_kit_item_kit", columnList = "kit_id"))
public class DIYKitItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kit_id", nullable = false)
    private DIYKit kit;

    @Column(nullable = false, length = 180)
    private String itemName;

    protected DIYKitItem() { }

    public DIYKitItem(DIYKit kit, String itemName) {
        this.kit = kit;
        this.itemName = itemName;
    }

    public Long getId() { return id; }
    public DIYKit getKit() { return kit; }
    public String getItemName() { return itemName; }
}
