package com.ecommerce.mobile.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.ecommerce.mobile.dto.manager.ManagerProductForm;
import com.ecommerce.mobile.dto.manager.ManagerProductImageForm;
import com.ecommerce.mobile.dto.manager.ManagerProductVariantForm;
import com.ecommerce.mobile.entity.Category;
import com.ecommerce.mobile.entity.Product;
import com.ecommerce.mobile.entity.ProductImage;
import com.ecommerce.mobile.entity.ProductVariant;
import com.ecommerce.mobile.enums.ProductStatus;
import com.ecommerce.mobile.repository.CategoryRepository;
import com.ecommerce.mobile.repository.ProductImageRepository;
import com.ecommerce.mobile.repository.ProductRepository;
import com.ecommerce.mobile.repository.ProductVariantRepository;

@Service
public class ManagerService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;

    public ManagerService(CategoryRepository categoryRepository,
                          ProductRepository productRepository,
                          ProductVariantRepository productVariantRepository,
                          ProductImageRepository productImageRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.productImageRepository = productImageRepository;
    }

    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        return productRepository.findAllWithCategory(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    @Transactional(readOnly = true)
    public ManagerProductForm newProductForm() {
        ManagerProductForm form = new ManagerProductForm();
        ensureAtLeastOneVariant(form);
        return form;
    }

    @Transactional(readOnly = true)
    public ManagerProductForm loadProductForm(Long productId) {
        Product product = productRepository.findDetailedByProductId(productId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy product id = " + productId));
        return toForm(product);
    }

    @Transactional(readOnly = true)
    public Product getProductDetail(Long productId) {
        return productRepository.findDetailedByProductId(productId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy product id = " + productId));
    }

    @Transactional
    public Product saveProduct(ManagerProductForm form) {
        Category category = categoryRepository.findById(form.getCategoryId())
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy category id = " + form.getCategoryId()));

        Product product = form.getProductId() == null
                ? new Product()
                : productRepository.findById(form.getProductId())
                    .orElseThrow(() -> new NoSuchElementException("Không tìm thấy product id = " + form.getProductId()));

        product.setName(form.getName());
        product.setBrand(form.getBrand());
        product.setDescription(form.getDescription());
        product.setCategory(category);
        product.setStatus(form.getStatus() == null ? ProductStatus.ACTIVE : form.getStatus());
        product = productRepository.save(product);

        replaceProductTree(product, form.getVariants());
        return productRepository.findDetailedByProductId(product.getProductId())
                .orElse(product);
    }

    @Transactional
    public Product softDeleteProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy product id = " + productId));
        product.setStatus(ProductStatus.INACTIVE);
        return productRepository.save(product);
    }

    @Transactional
    public Product restoreProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy product id = " + productId));
        product.setStatus(ProductStatus.ACTIVE);
        return productRepository.save(product);
    }

    private void replaceProductTree(Product product, List<ManagerProductVariantForm> variantForms) {
        Long productId = product.getProductId();
        productImageRepository.deleteByProductId(productId);
        productVariantRepository.deleteByProductId(productId);

        List<ManagerProductVariantForm> safeVariants = variantForms == null ? List.of() : variantForms;
        for (ManagerProductVariantForm variantForm : safeVariants) {
            if (isBlankVariant(variantForm)) {
                continue;
            }

            ProductVariant variant = new ProductVariant();
            variant.setProduct(product);
            variant.setSku(variantForm.getSku());
            variant.setStorage_gb(variantForm.getStorageGb());
            variant.setPrice(variantForm.getPrice());
            variant.setImportPrice(variantForm.getImportPrice());
            variant.setStockQty(variantForm.getStockQty());
            variant = productVariantRepository.save(variant);

            List<ManagerProductImageForm> validImages = normalizeImages(variantForm.getImages());
            for (ManagerProductImageForm imageForm : validImages) {
                ProductImage image = new ProductImage();
                image.setVariant(variant);
                image.setUrl(imageForm.getUrl());
                image.setIsPrimary(Boolean.TRUE.equals(imageForm.getIsPrimary()));
                productImageRepository.save(image);
            }
        }
    }

    private ManagerProductForm toForm(Product product) {
        ManagerProductForm form = new ManagerProductForm();
        form.setProductId(product.getProductId());
        form.setCategoryId(product.getCategory() == null ? null : product.getCategory().getCategoryId());
        form.setName(product.getName());
        form.setBrand(product.getBrand());
        form.setDescription(product.getDescription());
        form.setStatus(product.getStatus());

        List<ManagerProductVariantForm> variantForms = new ArrayList<>();
        if (product.getVariants() != null) {
            List<ProductVariant> variants = new ArrayList<>(product.getVariants());
            variants.sort(Comparator.comparing(v -> v.getVariant_id() == null ? Long.MAX_VALUE : v.getVariant_id()));
            for (ProductVariant variant : variants) {
                ManagerProductVariantForm variantForm = new ManagerProductVariantForm();
                variantForm.setVariantId(variant.getVariant_id());
                variantForm.setStorageGb(variant.getStorage_gb());
                variantForm.setPrice(variant.getPrice());
                variantForm.setImportPrice(variant.getImportPrice());
                variantForm.setStockQty(variant.getStockQty());
                variantForm.setSku(variant.getSku());

                List<ManagerProductImageForm> imageForms = new ArrayList<>();
                if (variant.getImages() != null) {
                    variant.getImages().forEach(image -> {
                        ManagerProductImageForm imageForm = new ManagerProductImageForm();
                        imageForm.setImageId(image.getImageId());
                        imageForm.setUrl(image.getUrl());
                        imageForm.setIsPrimary(image.getIsPrimary());
                        imageForms.add(imageForm);
                    });
                }
                if (imageForms.isEmpty()) {
                    imageForms.add(new ManagerProductImageForm());
                }
                variantForm.setImages(imageForms);
                variantForms.add(variantForm);
            }
        }

        if (variantForms.isEmpty()) {
            ensureAtLeastOneVariant(form);
        } else {
            form.setVariants(variantForms);
        }

        return form;
    }

    private void ensureAtLeastOneVariant(ManagerProductForm form) {
        if (form.getVariants() == null) {
            form.setVariants(new ArrayList<>());
        }
        if (form.getVariants().isEmpty()) {
            ManagerProductVariantForm variantForm = new ManagerProductVariantForm();
            variantForm.getImages().add(new ManagerProductImageForm());
            form.getVariants().add(variantForm);
        }
    }

    private boolean isBlankVariant(ManagerProductVariantForm form) {
        if (form == null) {
            return true;
        }
        boolean coreBlank = form.getStorageGb() == null
                && form.getPrice() == null
                && form.getImportPrice() == null
                && form.getStockQty() == null
                && !StringUtils.hasText(form.getSku());
        boolean imagesBlank = form.getImages() == null || form.getImages().stream().allMatch(this::isBlankImage);
        return coreBlank && imagesBlank;
    }

    private List<ManagerProductImageForm> normalizeImages(List<ManagerProductImageForm> images) {
        List<ManagerProductImageForm> result = new ArrayList<>();
        if (images == null) {
            return result;
        }

        for (ManagerProductImageForm image : images) {
            if (!isBlankImage(image)) {
                result.add(image);
            }
        }

        if (result.isEmpty()) {
            return result;
        }

        boolean hasPrimary = result.stream().anyMatch(img -> Boolean.TRUE.equals(img.getIsPrimary()));
        if (!hasPrimary) {
            result.get(0).setIsPrimary(true);
        } else {
            boolean primaryAssigned = false;
            for (ManagerProductImageForm image : result) {
                if (Boolean.TRUE.equals(image.getIsPrimary())) {
                    if (!primaryAssigned) {
                        primaryAssigned = true;
                    } else {
                        image.setIsPrimary(false);
                    }
                }
            }
        }

        return result;
    }

    private boolean isBlankImage(ManagerProductImageForm form) {
        return form == null || !StringUtils.hasText(form.getUrl());
    }
}
