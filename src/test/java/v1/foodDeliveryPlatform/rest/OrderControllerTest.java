package v1.foodDeliveryPlatform.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import v1.foodDeliveryPlatform.config.ControllerTestSecurityConfig;
import v1.foodDeliveryPlatform.dto.model.ItemDto;
import v1.foodDeliveryPlatform.dto.model.OrderDto;
import v1.foodDeliveryPlatform.dto.model.PaymentDto;
import v1.foodDeliveryPlatform.facade.ItemFacade;
import v1.foodDeliveryPlatform.facade.OrderFacade;
import v1.foodDeliveryPlatform.facade.PaymentFacade;
import v1.foodDeliveryPlatform.model.enums.PaymentMethod;
import v1.foodDeliveryPlatform.model.enums.PaymentStatus;
import v1.foodDeliveryPlatform.model.feign.DishClient;
import v1.foodDeliveryPlatform.model.feign.RestaurantClient;
import v1.foodDeliveryPlatform.security.SecurityUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@Import(ControllerTestSecurityConfig.class)
@TestPropertySource(properties = {
        "restaurant.service.url=http://restaurant:8080/api/v1",
        "RESTAURANT_ENDPOINT=http://restaurant:8080/api/v1"
})
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderFacade orderFacade;

    @MockitoBean
    private ItemFacade itemFacade;

    @MockitoBean
    private PaymentFacade paymentFacade;

    @MockitoBean
    private SecurityUtils expression;

    @MockitoBean
    private RestaurantClient restaurantClient;

    @MockitoBean
    private DishClient dishClient;

    private final UUID orderId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID restaurantId = UUID.randomUUID();
    private final UUID itemId = UUID.randomUUID();
    private final UUID paymentId = UUID.randomUUID();

    private String paymentRequestJson;

    @BeforeEach
    void setUp() {
        paymentRequestJson = """
            {
                "method": "CREDIT_CARD"
            }
            """;
    }

    private final String itemsJson = """
        [
            {
                "id": "%s",
                "dish_id": "%s",
                "quantity": 2,
                "price": 25.50
            }
        ]
        """.formatted(itemId, UUID.randomUUID());

    private final String itemJson = """
        {
            "id": "%s",
            "dish_id": "%s",
            "quantity": 1,
            "price": 15.75
        }
        """.formatted(itemId, UUID.randomUUID());

    @Test
    @WithMockUser(authorities = {"ROLE_ADMIN"})
    void getById_WithAdminRole_Success() throws Exception {
        OrderDto orderDto = new OrderDto();
        when(orderFacade.getById(orderId)).thenReturn(orderDto);
        when(expression.isAccessOrder(orderId, null)).thenReturn(true);

        mockMvc.perform(get("/api/v1/orders/{id}", orderId))
                .andExpect(status().isOk());

        verify(orderFacade).getById(orderId);
    }

    @Test
    @WithMockUser(authorities = {"ROLE_USER"})
    void getById_WithUserRole_WithAccess_Success() throws Exception {
        OrderDto orderDto = new OrderDto();
        when(orderFacade.getById(orderId)).thenReturn(orderDto);
        when(expression.isAccessOrder(eq(orderId), any())).thenReturn(true);

        mockMvc.perform(get("/api/v1/orders/{id}", orderId))
                .andExpect(status().isOk());

        verify(orderFacade).getById(orderId);
    }

    @Test
    @WithMockUser(authorities = {"ROLE_USER"})
    void getById_WithUserRole_NoAccess_Forbidden() throws Exception {
        when(expression.isAccessOrder(orderId, null)).thenReturn(false);

        mockMvc.perform(get("/api/v1/orders/{id}", orderId))
                .andExpect(status().isForbidden());

        verify(orderFacade, never()).getById(any());
    }

    @Test
    void getById_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/orders/{id}", orderId))
                .andExpect(status().isUnauthorized());

        verify(orderFacade, never()).getById(any());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_ADMIN"})
    void deleteById_WithAdminRole_Success() throws Exception {
        doNothing().when(orderFacade).delete(orderId);
        when(expression.isAccessOrder(orderId, null)).thenReturn(true);

        mockMvc.perform(delete("/api/v1/orders/{id}", orderId)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(orderFacade).delete(orderId);
    }

    @Test
    @WithMockUser(authorities = {"ROLE_USER"})
    void deleteById_WithUserRole_NoAccess_Forbidden() throws Exception {
        when(expression.isAccessOrder(orderId, null)).thenReturn(false);

        mockMvc.perform(delete("/api/v1/orders/{id}", orderId)
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(orderFacade, never()).delete(any());
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void createOrder_AuthenticatedUser_Success() throws Exception {
        OrderDto orderDto = new OrderDto();
        when(orderFacade.createOrder(eq(restaurantId), anyList())).thenReturn(orderDto);

        mockMvc.perform(post("/api/v1/orders")
                        .with(csrf())
                        .param("restaurantId", restaurantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemsJson))
                .andExpect(status().isCreated());

        verify(orderFacade).createOrder(eq(restaurantId), anyList());
    }

    @Test
    void createOrder_Unauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .with(csrf())
                        .param("restaurantId", restaurantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemsJson))
                .andExpect(status().isUnauthorized());

        verify(orderFacade, never()).createOrder(any(), any());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_ADMIN"})
    void getAll_WithAdminRole_Success() throws Exception {
        List<OrderDto> orders = List.of(new OrderDto());
        when(orderFacade.getAll()).thenReturn(orders);

        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isOk());

        verify(orderFacade).getAll();
    }

    @Test
    @WithMockUser(authorities = {"ROLE_USER"})
    void getAll_WithUserRole_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isForbidden());

        verify(orderFacade, never()).getAll();
    }

    @Test
    @WithMockUser(authorities = {"ROLE_ADMIN"})
    void getAllByUserId_WithAdminRole_Success() throws Exception {
        List<OrderDto> orders = List.of(new OrderDto());
        when(orderFacade.getAllByUserId(userId)).thenReturn(orders);
        when(expression.isAccessUser(userId, null)).thenReturn(true);

        mockMvc.perform(get("/api/v1/orders/user/{userId}", userId))
                .andExpect(status().isOk());

        verify(orderFacade).getAllByUserId(userId);
    }

    @Test
    @WithMockUser(authorities = {"ROLE_USER"})
    void getAllByUserId_WithUserRole_NoAccess_Forbidden() throws Exception {
        when(expression.isAccessUser(userId, null)).thenReturn(false);

        mockMvc.perform(get("/api/v1/orders/user/{userId}", userId))
                .andExpect(status().isForbidden());

        verify(orderFacade, never()).getAllByUserId(any());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_ADMIN"})
    void updateOrderStatus_WithAdminRole_Success() throws Exception {
        OrderDto orderDto = new OrderDto();
        when(orderFacade.updateOrderStatus(orderId)).thenReturn(orderDto);

        mockMvc.perform(patch("/api/v1/orders/{id}/status", orderId)
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(orderFacade).updateOrderStatus(orderId);
    }

    @Test
    @WithMockUser(authorities = {"ROLE_USER"})
    void updateOrderStatus_WithUserRole_Forbidden() throws Exception {
        mockMvc.perform(patch("/api/v1/orders/{id}/status", orderId)
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(orderFacade, never()).updateOrderStatus(any());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_USER"})
    void createItem_WithAccess_Success() throws Exception {
        ItemDto itemDto = new ItemDto();
        when(itemFacade.createItem(any(ItemDto.class), eq(orderId))).thenReturn(itemDto);
        when(expression.isAccessOrder(eq(orderId), any())).thenReturn(true);

        mockMvc.perform(post("/api/v1/orders/{id}/items", orderId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemJson))
                .andExpect(status().isCreated());

        verify(itemFacade).createItem(any(ItemDto.class), eq(orderId));
    }

    @Test
    @WithMockUser
    void createItem_NoAccess_Forbidden() throws Exception {
        when(expression.isAccessOrder(orderId, null)).thenReturn(false);

        mockMvc.perform(post("/api/v1/orders/{id}/items", orderId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemJson))
                .andExpect(status().isForbidden());

        verify(itemFacade, never()).createItem(any(), any());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_ADMIN"})
    void getItemsByOrderId_WithAdminRole_Success() throws Exception {
        List<ItemDto> items = List.of(new ItemDto());
        when(itemFacade.getAllByOrderId(orderId)).thenReturn(items);
        when(expression.isAccessOrder(orderId, null)).thenReturn(true);

        mockMvc.perform(get("/api/v1/orders/{id}/items", orderId))
                .andExpect(status().isOk());

        verify(itemFacade).getAllByOrderId(orderId);
    }

    @Test
    @WithMockUser(authorities = {"ROLE_USER"})
    void getItemsByOrderId_WithUserRole_NoAccess_Forbidden() throws Exception {
        when(expression.isAccessOrder(orderId, null)).thenReturn(false);

        mockMvc.perform(get("/api/v1/orders/{id}/items", orderId))
                .andExpect(status().isForbidden());

        verify(itemFacade, never()).getAllByOrderId(any());
    }

    @Test
    @WithMockUser
    void createOrder_ValidationError() throws Exception {
        String invalidItemsJson = """
        [
            {
                "id": "%s",
                "dish_id": null,
                "quantity": -1,
                "price": -10.0
            }
        ]
        """.formatted(itemId);

        mockMvc.perform(post("/api/v1/orders")
                        .with(csrf())
                        .param("restaurantId", restaurantId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidItemsJson))
                .andExpect(status().isBadRequest());

        verify(orderFacade, never()).createOrder(any(), any());
    }
}