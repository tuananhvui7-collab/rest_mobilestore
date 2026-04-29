package com.ecommerce.mobile.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ecommerce.mobile.entity.Product;
import com.ecommerce.mobile.enums.ProductStatus;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {


    // Chỉ fetch category + variants: thêm variants.images cùng lúc gây MultipleBagFetchException với List (bag).
    @EntityGraph(attributePaths = {"category", "variants"})
    Page<Product> findByStatus(ProductStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "variants"})
    @Query("""
                select  p 
                from Product p
                where p.status = :status
                and ( lower(p.name) like lower(concat('%', :keyword ,'%'))
                or lower(p.brand) like lower(concat('%', :keyword, '%'))
                )
                    """) //query để làm cho keyword
    Page<Product> searchByStatusAndKeyword(
        @Param("status")ProductStatus status, 
        @Param("keyword") String keyword,
         Pageable pageable);


    Page<Product> findByNameContainingIgnoreCaseOrBrandContainingIgnoreCase( // tìm sản phẩm bằng tất cả kí tự name, brand, có phân trang
            String name, String brand, Pageable pageable);

    List<Product> findByCategoryCategoryIdAndStatus(Long categoryId, ProductStatus status); // tìm sản phẩm bằng categoryId và status

    List<Product> findByStatus(ProductStatus status); // tìm sản phẩm bằng status

    List<Product> findByBrandAndStatus(String brand, ProductStatus status); // tìm sản phẩm bằng hãng và status

    List<Product> findByCategoryCategoryId(Long categoryId); // tìm bằng categoryId

    Product findByProductId(Long productId);

    @EntityGraph(attributePaths = {"category", "variants"})
    @Query("select p from Product p where p.productId = :productId")
    Optional<Product> findDetailedByProductId(@Param("productId") Long productId);

    @EntityGraph(attributePaths = {"category"})
    @Query("select p from Product p")
    List<Product> findAllWithCategory(Sort sort);
}
