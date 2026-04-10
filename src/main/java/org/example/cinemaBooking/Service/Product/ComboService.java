package org.example.cinemaBooking.Service.Product;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.example.cinemaBooking.DTO.Request.Combo.ComboItemRequest;
import org.example.cinemaBooking.DTO.Request.Combo.CreateComboRequest;
import org.example.cinemaBooking.DTO.Request.Combo.UpdateComboRequest;
import org.example.cinemaBooking.DTO.Response.Combo.ComboResponse;
import org.example.cinemaBooking.Entity.Combo;
import org.example.cinemaBooking.Entity.ComboItem;
import org.example.cinemaBooking.Entity.Product;
import org.example.cinemaBooking.Exception.AppException;
import org.example.cinemaBooking.Exception.ErrorCode;
import org.example.cinemaBooking.Mapper.ComboMapper;
import org.example.cinemaBooking.Repository.ComboRepository;
import org.example.cinemaBooking.Repository.ProductRepository;
import org.example.cinemaBooking.Shared.response.PageResponse;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@FieldDefaults(level = lombok.AccessLevel.PRIVATE, makeFinal = true)
public class ComboService {
    ComboRepository comboRepository;
    ProductRepository productRepository;
    ComboMapper comboMapper;

    private void updateComboData(Combo combo, CreateComboRequest request) {
        combo.setName(request.name());
        combo.setPrice(request.price());
        combo.setImage(request.image());
        combo.setDescription(request.description());

        combo.getItems().clear();
        comboRepository.flush();
        addItemsToCombo(combo, request.items());
    }

    private void addItemsToCombo(Combo combo, List<ComboItemRequest> request) {
        if (request == null || request.isEmpty()) return;
        List<String> ids= request.stream()
                        .map(ComboItemRequest::productId)
                        .toList();
        List<Product> products = productRepository.findAllById(ids);
        Map<String, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        for(ComboItemRequest itemRequest : request){
            Product product = productMap.get(itemRequest.productId());
            if(product == null || product.isDeleted()){
                throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
            }
            ComboItem comboItem = ComboItem.builder()
                    .quantity(itemRequest.quantity())
                    .product(product)
                    .combo(combo)
                    .build();
            combo.addItem(comboItem);
        }
    }

    private void validateNoDuplicateProducts(List<ComboItemRequest> items) {
        if (items == null) return;

        Set<String> ids = new HashSet<>();

        for (ComboItemRequest item : items) {
            if (!ids.add(item.productId())) {
                throw new AppException(ErrorCode.DUPLICATE_PRODUCT_IN_COMBO);
            }
        }
    }

    /**
     * Tạo mới một combo.
     * <p>Xoá bộ nhớ đệm của các danh sách combo.</p>
     *
     * @param request Thông tin tạo mới combo
     * @return ComboResponse thông tin combo vừa tạo
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "combos", allEntries = true),
            @CacheEvict(value = "combos-active", allEntries = true)
    })
    public ComboResponse createCombo(CreateComboRequest request) {
        validateNoDuplicateProducts(request.items());
        Optional<Combo> existingCombo = comboRepository.findByName(request.name());
        Combo combo;
        if (existingCombo.isPresent()) {
            combo = existingCombo.get();
            if(!combo.isDeleted()){
                throw new AppException(ErrorCode.COMBO_ALREADY_EXISTS);
            }
            // nếu combo đã bị xóa mềm, ta sẽ khôi phục nó và cập nhật lại thông tin
            combo.restore();
            combo.setActive(true);
            updateComboData(combo, request);
        }
        else {
            combo = comboMapper.toEntity(request);
            addItemsToCombo(combo, request.items());
        }
        Combo savedCombo = comboRepository.save(combo);
        return comboMapper.toResponse(savedCombo);
    }

    /**
     * Cập nhật thông tin combo.
     * <p>Xoá bộ nhớ đệm danh sách combo và chi tiết combo tương ứng.</p>
     *
     * @param comboId ID của combo cần cập nhật
     * @param request Thông tin cập nhật mới
     * @return ComboResponse thông tin combo sau cập nhật
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "combos", allEntries = true),
            @CacheEvict(value = "combos-active", allEntries = true),
            @CacheEvict(value = "combo", key = "#comboId")
    })
    public ComboResponse updateCombo(String comboId, UpdateComboRequest request) {
        Combo combo = comboRepository.findByIdWithDetail(comboId)
                .orElseThrow(() -> new AppException(ErrorCode.COMBO_NOT_FOUND));

        if(request.name() != null && !request.name().isEmpty()){
            Optional<Combo> existingCombo = comboRepository.findByName(request.name());
            if(existingCombo.isPresent() && !existingCombo.get().getId().equals(comboId) && !existingCombo.get().isDeleted()){
                throw new AppException(ErrorCode.COMBO_ALREADY_EXISTS);
            }
            combo.setName(request.name());
        }

        comboMapper.updateCombo(request, combo);

        if (request.items() != null) {
            validateNoDuplicateProducts(request.items());
            combo.getItems().clear();
            comboRepository.flush();
            addItemsToCombo(combo, request.items());
        }

        Combo saved = comboRepository.save(combo);
        return comboMapper.toResponse(saved);
    }

    /**
     * Xoá mềm một combo.
     * <p>Xoá bộ nhớ đệm danh sách combo và chi tiết combo.</p>
     *
     * @param comboId ID của combo cần xoá
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "combos", allEntries = true),
            @CacheEvict(value = "combos-active", allEntries = true),
            @CacheEvict(value = "combo", key = "#comboId")
    })
    public void deleteCombo(String comboId) {
        Combo combo = comboRepository.findById(comboId)
                .orElseThrow(() -> new AppException(ErrorCode.COMBO_NOT_FOUND));
        combo.softDelete();
        combo.setActive(false);
        comboRepository.save(combo);
    }

    /**
     * Lấy thông tin chi tiết của một combo.
     * <p>Dữ liệu được lưu trữ ở bộ đệm "combo".</p>
     *
     * @param comboId ID của combo
     * @return ComboResponse thông tin chi tiết
     */
    @Cacheable(value = "combo", key = "#comboId")
    public ComboResponse getComboById(String comboId) {
        Combo combo = comboRepository.findByIdWithDetail(comboId)
                .orElseThrow(() -> new AppException(ErrorCode.COMBO_NOT_FOUND));
        return comboMapper.toResponse(combo);
    }

    /**
     * Lấy danh sách combo (có phân trang và tìm kiếm).
     * <p>Danh sách được lưu vào bộ đệm "combos".</p>
     *
     * @param page      Số thứ tự trang
     * @param size      Kích thước trang
     * @param keyword   Từ khoá tìm kiếm theo tên
     * @param direction Hướng sắp xếp (asc/desc)
     * @param sortBy    Cột cần sắp xếp
     * @return PageResponse hiển thị danh sách combo
     */
    @Cacheable(value = "combos",
            key = "#page + '-' + #size + '-' + (#keyword ?: '') + '-' + #direction + '-' + #sortBy")
    public PageResponse<ComboResponse> getCombos(
            int page,
            int size,
            String keyword,
            String direction,
            String sortBy
    ) {
        int pageNumber = page > 0 ? page - 1 : 0;
        Sort.Direction sortDirection = direction.equals("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        Pageable pageble = PageRequest.of(pageNumber, size, Sort.by(sortDirection, sortBy));

        Page<Combo> pageCombos = (keyword == null || keyword.isEmpty())
                ? comboRepository.findByDeletedFalse(pageble)
                : comboRepository.findByNameContainingIgnoreCaseAndDeletedFalse(keyword,  pageble);

        List<ComboResponse> comboResponses = pageCombos.getContent().stream()
                .map(comboMapper::toResponse)
                .toList();
        return PageResponse.<ComboResponse>builder()
                .page(page)
                .size(size)
                .totalElements(pageCombos.getTotalElements())
                .totalPages(pageCombos.getTotalPages())
                .items(comboResponses)
                .build();
    }

    /**
     * Đổi trạng thái hoạt động của combo (Active/Inactive).
     * <p>Xóa bộ nhớ đệm để cập nhật hiển thị.</p>
     *
     * @param comboId ID của combo
     */
    @Caching(evict = {
            @CacheEvict(value = "combos", allEntries = true),
            @CacheEvict(value = "combos-active", allEntries = true),
            @CacheEvict(value = "combo", key = "#comboId")
    })
    public void toggleActiveCombo(String comboId) {
        Combo combo = comboRepository.findById(comboId)
                .orElseThrow(() -> new AppException(ErrorCode.COMBO_NOT_FOUND));
        combo.setActive(!combo.getActive());
        comboRepository.save(combo);
    }

    /**
     * Lấy danh sách các combo hiện đang Active (cho người dùng xem).
     * <p>Danh sách được lưu vào bộ đệm "combos-active".</p>
     *
     * @param page      Số trang
     * @param size      Kích thước
     * @param sortBy    Cột sắp xếp
     * @param direction Hướng sắp xếp
     * @return PageResponse các combo đang hiển thị
     */
    @Cacheable(value = "combos-active",
            key = "#page + '-' + #size + '-' + #sortBy + '-' + #direction")
    public PageResponse<ComboResponse>getAllCombosActive(int page, int size, String sortBy, String direction){
        int pageNumber = page > 0 ? page - 1 : 0;
        Sort.Direction sortDirection = direction.equals("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(pageNumber, size, Sort.by(sortDirection, sortBy));
        Page<Combo> pageCombos = comboRepository.findByActiveTrueAndDeletedFalse(pageable);

        List<ComboResponse> combos = pageCombos.getContent()
                .stream().map(comboMapper::toResponse)
                .toList();
        return PageResponse.<ComboResponse>builder()
                .page(page)
                .size(size)
                .totalElements(pageCombos.getTotalElements())
                .totalPages(pageCombos.getTotalPages())
                .items(combos)
                .build();
    }


}
