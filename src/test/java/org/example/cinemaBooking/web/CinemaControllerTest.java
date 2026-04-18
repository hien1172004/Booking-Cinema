package org.example.cinemaBooking.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.cinemaBooking.Config.SecurityConfig;
import org.example.cinemaBooking.Controller.CinemaController;
import org.example.cinemaBooking.DTO.Request.Cinema.CreateCinemaRequest;
import org.example.cinemaBooking.DTO.Request.Cinema.UpdateCinemaRequest;
import org.example.cinemaBooking.DTO.Response.Cinema.CinemaMovieResponse;
import org.example.cinemaBooking.DTO.Response.Cinema.CinemaResponse;
import org.example.cinemaBooking.DTO.Response.Room.RoomBasicResponse;
import org.example.cinemaBooking.Exception.AppException;
import org.example.cinemaBooking.Exception.ErrorCode;
import org.example.cinemaBooking.Service.Cinema.CinemaService;
import org.example.cinemaBooking.Service.RateLimit.RateLimitService;
import org.example.cinemaBooking.Shared.enums.Status;
import org.example.cinemaBooking.Shared.response.PageResponse;
import org.example.cinemaBooking.config.TestSecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(
        controllers = CinemaController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {
                        org.example.cinemaBooking.Config.RateLimitFilter.class
                }
        )
)
@Import(TestSecurityConfig.class)
public class CinemaControllerTest {

    @MockBean
    private CinemaService cinemaService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private final String BASE_URL = "/api/v1/cinema";
    private CreateCinemaRequest validCreateRequest;
    private CinemaResponse mockCinemaResponse;
    private UpdateCinemaRequest validUpdateRequest;

    @BeforeEach
    void setUp() {
        validCreateRequest = CreateCinemaRequest.builder()
                .name("CGV Vincom Center")
                .address("123 Đường Lê Lợi, Quận 1, TP.HCM")
                .phone("02812345678")
                .hotline("19001234")
                .logoUrl("https://example.com/logo.png")
                .build();

        validUpdateRequest = UpdateCinemaRequest.builder()
                .name("CGV Vincom Center Updated")
                .address("456 Đường Nguyễn Huệ")
                .phone("02887654321")
                .hotline("19005678")
                .logoUrl("https://example.com/logo-new.png")
                .build();

        mockCinemaResponse = CinemaResponse.builder()
                .id("123e4567-e89b-12d3-a456-426614174000")
                .name("CGV Vincom Center")
                .address("123 Đường Lê Lợi, Quận 1, TP.HCM")
                .phone("02812345678")
                .hotline("19001234")
                .logoUrl("https://example.com/logo.png")
                .status(Status.ACTIVE)
                .build();
    }

    @Nested
    class CreateCinemaTests {  // SỬA: CreatCinemaTests -> CreateCinemaTests

        @Test
        @WithMockUser(roles = "ADMIN")
        void createCinema_ShouldReturnSuccess_WhenAdminValidRequest() throws Exception {  // SỬA: JsonProcessingException -> Exception
            when(cinemaService.createCinema(any(CreateCinemaRequest.class)))
                    .thenReturn(mockCinemaResponse);

            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCreateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Cinema created successfully"))
                    .andExpect(jsonPath("$.data.id").value("123e4567-e89b-12d3-a456-426614174000"))
                    .andExpect(jsonPath("$.data.name").value("CGV Vincom Center"))
                    .andExpect(jsonPath("$.data.status").value(Status.ACTIVE.toString()));

        }

        @Test
        @WithMockUser(roles = "USER")
        void createCinema_ShouldReturnForbidden_WhenUserRole() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCreateRequest)))
                    .andExpect(status().isForbidden());
        }

        @Test
        void createCinema_ShouldReturnUnauthorized_WhenNotLoggedIn() throws Exception {
            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCreateRequest)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void createCinema_ShouldReturnError_WhenServiceThrowsException() throws Exception {
            when(cinemaService.createCinema(any(CreateCinemaRequest.class)))
                    .thenThrow(new AppException(ErrorCode.UNCAUGHT_EXCEPTION));

            mockMvc.perform(post(BASE_URL)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validCreateRequest)))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    class UpdateCinemaTests {
        private final String cinemaId = "123e4567-e89b-12d3-a456-426614174000";
        @Test
        @WithMockUser(roles = "ADMIN")
        void updateCinema_ShouldReturnSuccess_WhenAdminAndValidRequest() throws Exception {
            CinemaResponse updatedResponse = CinemaResponse.builder()
                    .id(cinemaId)
                    .name("CGV Vincom Center Updated")
                    .address("456 Đường Nguyễn Huệ")
                    .phone("02887654321")
                    .hotline("19005678")
                    .logoUrl("https://example.com/logo-new.png")
                    .build();

            when(cinemaService.updateCinema(eq(cinemaId), any(UpdateCinemaRequest.class)))
                    .thenReturn(updatedResponse);

            mockMvc.perform(put(BASE_URL + "/{id}", cinemaId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUpdateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Cinema updated successfully"))
                    .andExpect(jsonPath("$.data.name").value("CGV Vincom Center Updated"))
                    .andExpect(jsonPath("$.data.address").value("456 Đường Nguyễn Huệ"));
        }


        @Test
        @WithMockUser(roles = "USER")
        void updateCinema_ShouldReturnForbidden_WhenNotAdmin() throws Exception {
            mockMvc.perform(put(BASE_URL + "/{id}", cinemaId)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUpdateRequest)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void updateCinema_ShouldReturnNotFound_WhenCinemaDoesNotExist() throws Exception {
            when(cinemaService.updateCinema(eq("non-existent-id"), any(UpdateCinemaRequest.class)))
                    .thenThrow(new AppException(ErrorCode.CINEMA_NOT_FOUND));

            mockMvc.perform(put(BASE_URL + "/{id}", "non-existent-id")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(validUpdateRequest)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class DeleteCinemaTests {
        private final String cinemaId = "123e4567-e89b-12d3-a456-426614174000";

        @Test
        @WithMockUser(roles = "ADMIN")
        void deleteCinema_ShouldReturnSuccess_WhenAdmin() throws Exception {
            doNothing().when(cinemaService).deleteCinemaById(cinemaId);

            mockMvc.perform(delete(BASE_URL + "/{id}", cinemaId)
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Cinema deleted successfully"));
        }

        @Test
        @WithMockUser(roles = "USER")
        void deleteCinema_ShouldReturnForbidden_WhenNotAdmin() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/{id}", cinemaId)
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        void deleteCinema_ShouldReturnError_WhenServiceThrowsException() throws Exception {
            doThrow(new AppException(ErrorCode.UNCAUGHT_EXCEPTION))
                    .when(cinemaService).deleteCinemaById(cinemaId);

            mockMvc.perform(delete(BASE_URL + "/{id}", cinemaId)
                            .with(csrf()))
                    .andExpect(status().isInternalServerError());
        }
    }
    // ==================== TEST GET CINEMA BY ID ====================
    @Nested
    class GetCinemaTests {

        private final String cinemaId = "123e4567-e89b-12d3-a456-426614174000";

        @Test
        void getCinema_ShouldReturnCinema_WhenExists() throws Exception {
            when(cinemaService.getCinemaById(cinemaId)).thenReturn(mockCinemaResponse);

            mockMvc.perform(get(BASE_URL + "/{id}", cinemaId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Cinema retrieved successfully"))
                    .andExpect(jsonPath("$.data.id").value(cinemaId))
                    .andExpect(jsonPath("$.data.name").value("CGV Vincom Center"));
        }

        @Test
        void getCinema_ShouldReturnNotFound_WhenCinemaDoesNotExist() throws Exception {
            when(cinemaService.getCinemaById("non-existent-id"))
                    .thenThrow(new AppException(ErrorCode.CINEMA_NOT_FOUND));

            mockMvc.perform(get(BASE_URL + "/{id}", "non-existent-id"))
                    .andExpect(status().isNotFound());
        }
    }
    @Nested
    class GetAllCinemasTests {

        @Test
        void getAllCinemas_ShouldReturnPageResponse_WithDefaultParams() throws Exception {
            PageResponse<CinemaResponse> mockPageResponse = PageResponse.<CinemaResponse>builder()
                    .items(List.of(mockCinemaResponse))
                    .page(1)
                    .size(10)
                    .totalElements(1L)
                    .totalPages(1)
                    .build();

            when(cinemaService.getAllCinemas(eq(1), eq(10), eq("createdAt"), eq("desc"), isNull()))
                    .thenReturn(mockPageResponse);

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Cinemas retrieved successfully"))
                    .andExpect(jsonPath("$.data.items.length()").value(1))
                    .andExpect(jsonPath("$.data.page").value(1))
                    .andExpect(jsonPath("$.data.totalPages").value(1));
        }

        @Test
        void getAllCinemas_ShouldSupportPaginationParams() throws Exception {
            PageResponse<CinemaResponse> mockPageResponse = PageResponse.<CinemaResponse>builder()
                    .items(List.of())
                    .page(2)
                    .size(5)
                    .totalElements(0L)
                    .totalPages(0)
                    .build();

            when(cinemaService.getAllCinemas(eq(2), eq(5), eq("name"), eq("asc"), isNull()))
                    .thenReturn(mockPageResponse);

            mockMvc.perform(get(BASE_URL)
                            .param("page", "2")
                            .param("size", "5")
                            .param("sortBy", "name")
                            .param("direction", "asc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.page").value(2))
                    .andExpect(jsonPath("$.data.size").value(5));
        }

        @Test
        void getAllCinemas_ShouldFilterByKeyword() throws Exception {
            PageResponse<CinemaResponse> mockPageResponse = PageResponse.<CinemaResponse>builder()
                    .items(List.of(mockCinemaResponse))
                    .build();

            when(cinemaService.getAllCinemas(eq(1), eq(10), eq("createdAt"), eq("desc"), eq("CGV")))
                    .thenReturn(mockPageResponse);

            mockMvc.perform(get(BASE_URL)
                            .param("keyword", "CGV"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }
    @Nested
    class ToggleStatusTests {

        private final String cinemaId = "123e4567-e89b-12d3-a456-426614174000";

        @Test
        @WithMockUser(roles = "ADMIN")
        void toggleStatus_ShouldReturnSuccess_WhenAdmin() throws Exception {
            CinemaResponse toggledResponse = CinemaResponse.builder()
                    .id(cinemaId)
                    .name("CGV Vincom Center")
                    .status(Status.INACTIVE)
                    .build();

            doNothing().when(cinemaService).toggleCinemaStatus(cinemaId);
            when(cinemaService.getCinemaById(cinemaId)).thenReturn(toggledResponse);

            mockMvc.perform(patch(BASE_URL + "/{id}/toggle-status", cinemaId)
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Cinema status toggled successfully"))
                    .andExpect(jsonPath("$.data.status").value(Status.INACTIVE.toString()));
        }

        @Test
        @WithMockUser(roles = "USER")
        void toggleStatus_ShouldReturnForbidden_WhenNotAdmin() throws Exception {
            mockMvc.perform(patch(BASE_URL + "/{id}/toggle-status", cinemaId)
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }
    @Nested
    class GetMoviesByCinemaAndDateTests {

        private final String cinemaId = "123e4567-e89b-12d3-a456-426614174000";
        private final LocalDate date = LocalDate.of(2025, 1, 15);

        @Test
        void getMoviesByCinemaAndDate_ShouldReturnMovies() throws Exception {
            CinemaMovieResponse movieResponse = CinemaMovieResponse.builder()
                    .movieId("movie-1")
                    .movieTitle("Avengers: Endgame")
                    .durationMinutes(180)
                    .build();

            PageResponse<CinemaMovieResponse> mockResponse = PageResponse.<CinemaMovieResponse>builder()
                    .items(List.of(movieResponse))
                    .page(0)
                    .size(10)
                    .totalElements(1L)
                    .totalPages(1)
                    .build();

            when(cinemaService.getMoviesByCinemaAndDate(eq(cinemaId), eq(date), eq(0), eq(10), eq("title"), eq("asc")))
                    .thenReturn(mockResponse);

            mockMvc.perform(get(BASE_URL + "/{cinemaId}/movies", cinemaId)
                            .param("date", date.toString())
                            .param("page", "0")
                            .param("size", "10")
                            .param("sortBy", "title")
                            .param("direction", "asc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Movies retrieved successfully"))
                    .andExpect(jsonPath("$.data.items[0].movieTitle").value("Avengers: Endgame"));
        }

        @Test
        void getMoviesByCinemaAndDate_ShouldUseDefaultPagination_WhenNotProvided() throws Exception {
            PageResponse<CinemaMovieResponse> mockResponse = new PageResponse<>();
            mockResponse.setItems(List.of());
            when(cinemaService.getMoviesByCinemaAndDate(eq(cinemaId), eq(date), eq(0), eq(10), eq("title"), eq("asc")))
                    .thenReturn(mockResponse);

            mockMvc.perform(get(BASE_URL + "/{cinemaId}/movies", cinemaId)
                            .param("date", date.toString()))
                    .andExpect(status().isOk());
        }
    }
    @Nested
    class GetRoomsByCinemaTests {

        private final String cinemaId = "123e4567-e89b-12d3-a456-426614174000";

        @Test
        void getRoomsByCinemaId_ShouldReturnRooms() throws Exception {
            RoomBasicResponse roomResponse = RoomBasicResponse.builder()
                    .id("room-1")
                    .name("Phòng 1")
                    .totalSeats(120)
                    .build();

            PageResponse<RoomBasicResponse> mockResponse = PageResponse.<RoomBasicResponse>builder()
                    .items(List.of(roomResponse))
                    .page(1)
                    .size(10)
                    .totalElements(1L)
                    .totalPages(1)
                    .build();

            when(cinemaService.getRoomsByCinema(eq(cinemaId), eq(1), eq(10), eq("name"), eq("asc")))
                    .thenReturn(mockResponse);

            mockMvc.perform(get(BASE_URL + "/{cinemaId}/rooms", cinemaId)
                            .param("page", "1")
                            .param("size", "10")
                            .param("sortBy", "name")
                            .param("direction", "asc"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Rooms retrieved successfully"))
                    .andExpect(jsonPath("$.data.items[0].name").value("Phòng 1"))
                    .andExpect(jsonPath("$.data.items[0].totalSeats").value(120));
        }

        @Test
        void getRoomsByCinemaId_ShouldUseDefaultPagination_WhenNotProvided() throws Exception {
            PageResponse<RoomBasicResponse> mockResponse = new PageResponse<>();
            mockResponse.setItems(List.of());

            when(cinemaService.getRoomsByCinema(eq(cinemaId), eq(1), eq(10), eq("name"), eq("asc")))
                    .thenReturn(mockResponse);

            mockMvc.perform(get(BASE_URL + "/{cinemaId}/rooms", cinemaId))
                    .andExpect(status().isOk());
        }
    }
}