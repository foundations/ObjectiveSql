package com.github.braisdom.objsql.example.domains;

import com.github.braisdom.objsql.annotations.DomainModel;
import com.github.braisdom.objsql.annotations.Relation;
import com.github.braisdom.objsql.relation.RelationType;

import java.math.BigDecimal;

@DomainModel(primaryClass = BigDecimal.class)
public class OrderLine {
    private String orderNo;
    private Float amount;
    private Float quantity;

    @Relation(relationType = RelationType.BELONGS_TO)
    private Order order;
}
