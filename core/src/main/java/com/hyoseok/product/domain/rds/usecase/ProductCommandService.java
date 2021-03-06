package com.hyoseok.product.domain.rds.usecase;

import com.hyoseok.product.domain.rds.entity.*;
import com.hyoseok.product.domain.rds.usecase.mapper.CreateProductImageMapper;
import com.hyoseok.product.domain.rds.usecase.mapper.ProductDescriptionTextMapper;
import com.hyoseok.product.domain.rds.usecase.mapper.ProductDescriptionVarcharMapper;
import com.hyoseok.product.domain.rds.usecase.mapper.ProductMapper;
import com.hyoseok.product.domain.rds.usecase.exception.ErrorMessage;
import com.hyoseok.product.domain.rds.usecase.exception.NotFoundProductException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static java.util.stream.Collectors.*;

@Service
@Transactional
@RequiredArgsConstructor
public class ProductCommandService {

    private final ProductRepository productRepository;
    private final ProductQueryRepository productQueryRepository;

    public Long create(ProductMapper productMapper,
                       ProductDescriptionTextMapper productDescriptionTextMapper,
                       ProductDescriptionVarcharMapper productDescriptionVarcharMapper,
                       CreateProductImageMapper productImageMapper) {
        Product product = createProduct(
                productMapper,
                createProductDescriptionText(productDescriptionTextMapper),
                createProductDescriptionVarchars(productDescriptionVarcharMapper),
                createProductImage(productImageMapper)
        );

        Product savedProduct = productRepository.save(product);

        return savedProduct.getId();
    }

    private ProductDescriptionText createProductDescriptionText(ProductDescriptionTextMapper mapper) {
        return ProductDescriptionText.create(mapper.getValue());
    }

    private List<ProductDescriptionVarchar> createProductDescriptionVarchars(ProductDescriptionVarcharMapper mapper) {
        List<String> keys = List.of("name", "user_code1", "user_code2", "hs_code", "weight", "volume_x",
                "volume_y", "volume_h", "production_date", "limit_date", "size_info", "material_info", "note");

        return keys.stream()
                .map(key -> ProductDescriptionVarchar.create(key, mapper.getDefaultAndAdditionalMap().get(key)))
                .collect(toList());
    }

    private List<ProductImage> createProductImage(CreateProductImageMapper mapper) {
        return mapper.getProductImageVOList().stream()
                .map(productImageVO ->
                        ProductImage.create(
                                productImageVO.getIsUrl(),
                                productImageVO.getKind(),
                                productImageVO.getImage(),
                                productImageVO.getSortOrder()
                        )
                ).collect(toList());
    }

    private Product createProduct(ProductMapper mapper,
                                  ProductDescriptionText productDescriptionText,
                                  List<ProductDescriptionVarchar> productDescriptionVarchars,
                                  List<ProductImage> productImages) {
        return Product.create(
                mapper.getIsSale(),
                mapper.getIsUsed(),
                mapper.getSupplierId(),
                mapper.getSupplyPrice(),
                mapper.getRecommendPrice(),
                mapper.getConsumerPrice(),
                mapper.getMaximum(),
                mapper.getMinimum(),
                productDescriptionText,
                productDescriptionVarchars,
                productImages
        );
    }

    public Product update(ProductMapper productMapper,
                       ProductDescriptionTextMapper productDescriptionTextMapper,
                       ProductDescriptionVarcharMapper productDescriptionVarcharMapper) {
        Product product = productQueryRepository.findWithFetchJoinById(productMapper.getId())
                .orElseThrow(() -> new NotFoundProductException(ErrorMessage.NOT_FOUND_PRODUCT_IN_DATABASE));

        changeProduct(product, productMapper);
        changeProductDescriptionText(product.getProductDescriptionText(), productDescriptionTextMapper);
        changeProductDescriptionVarchars(product.getProductDescriptionVarchars(), productDescriptionVarcharMapper);

        return product;
    }

    private void changeProduct(Product product, ProductMapper mapper) {
        product.change(
                mapper.getIsSale(),
                mapper.getIsUsed(),
                mapper.getSupplierId(),
                mapper.getSupplyPrice(),
                mapper.getRecommendPrice(),
                mapper.getConsumerPrice(),
                mapper.getMaximum(),
                mapper.getMinimum()
        );
    }

    private void changeProductDescriptionText(ProductDescriptionText productDescriptionText,
                                              ProductDescriptionTextMapper mapper) {
        productDescriptionText.change(mapper.getValue());
    }

    private void changeProductDescriptionVarchars(List<ProductDescriptionVarchar> productDescriptionVarchars,
                                                  ProductDescriptionVarcharMapper mapper) {
        productDescriptionVarchars.forEach(productDescriptionVarchar -> {
            String value = mapper.getDefaultAndAdditionalMap().get(productDescriptionVarchar.getKey());
            productDescriptionVarchar.change(value);
        });
    }

}
