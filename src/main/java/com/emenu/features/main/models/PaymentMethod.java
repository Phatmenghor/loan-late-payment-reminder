package com.emenu.features.main.models;

import com.emenu.enums.payment.PaymentMethodType;
import com.emenu.shared.domain.BaseUUIDEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "payment_methods")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentMethod extends BaseUUIDEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private PaymentMethodType type;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "account_name")
    private String accountName;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "qr_code_image_url")
    private String qrCodeImageUrl;

    @Column(name = "description")
    private String description;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    public boolean isCash() {
        return type == PaymentMethodType.CASH;
    }

    public boolean isBank() {
        return type == PaymentMethodType.BANK;
    }
}
