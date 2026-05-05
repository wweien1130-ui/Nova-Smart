package com.tianji.es.document;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Getter
@Setter
@Document(indexName = "items_vector")
public class ItemDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Text)
    private String name;

    @Field(type = FieldType.Integer)
    private Integer price;

    @Field(type = FieldType.Integer)
    private Integer stock;

    @Field(type = FieldType.Keyword)
    private String image;

    @Field(type = FieldType.Keyword)
    private String category;

    @Field(type = FieldType.Keyword)
    private String brand;

    @Field(type = FieldType.Text)
    private String spec;

    @Field(type = FieldType.Integer)
    private Integer sold;

    @Field(type = FieldType.Integer)
    @JsonProperty("comment_count")
    private Integer commentCount;

    @Field(type = FieldType.Boolean)
    @JsonProperty("isAD")
    private Boolean isAD;

    @Field(type = FieldType.Integer)
    private Integer status;

    @Field(type = FieldType.Date)
    @JsonProperty("create_time")
    private String createTime;

    @Field(type = FieldType.Date)
    @JsonProperty("update_time")
    private String updateTime;

    // 向量字段 - 使用 float[] 类型
    private float[] embedding;

    // ===== Getters & Setters =====

}
