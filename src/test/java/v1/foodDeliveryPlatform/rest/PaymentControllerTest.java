package v1.foodDeliveryPlatform.rest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import v1.foodDeliveryPlatform.config.ControllerTestSecurityConfig;
import v1.foodDeliveryPlatform.dto.model.PaymentDto;
import v1.foodDeliveryPlatform.exception.ResourceNotFoundException;
import v1.foodDeliveryPlatform.facade.PaymentFacade;
import v1.foodDeliveryPlatform.security.SecurityUtils;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@Import(ControllerTestSecurityConfig.class)
@TestPropertySource(properties = {
        "restaurant.service.url=http://restaurant:8080/api/v1",
        "RESTAURANT_ENDPOINT=http://restaurant:8080/api/v1"
})
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentFacade paymentFacade;

    @MockitoBean
    private SecurityUtils expression;

    private final UUID paymentId = UUID.randomUUID();

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void getById_WithAdminRole_Success() throws Exception {
        PaymentDto paymentDto = new PaymentDto();
        when(paymentFacade.getById(paymentId)).thenReturn(paymentDto);

        mockMvc.perform(get("/api/v1/payment/{id}", paymentId))
                .andExpect(status().isOk());

        verify(paymentFacade).getById(paymentId);
        verify(expression, never()).isAccessPayment(any(), any());
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void getById_WithUserRole_WithAccess_Success() throws Exception {
        PaymentDto paymentDto = new PaymentDto();
        when(paymentFacade.getById(paymentId)).thenReturn(paymentDto);
        when(expression.isAccessPayment(eq(paymentId), any())).thenReturn(true);

        mockMvc.perform(get("/api/v1/payment/{id}", paymentId))
                .andExpect(status().isOk());

        verify(paymentFacade).getById(paymentId);
        verify(expression).isAccessPayment(eq(paymentId), any());
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void getById_WithUserRole_NoAccess_Forbidden() throws Exception {
        when(expression.isAccessPayment(eq(paymentId), any())).thenReturn(false);

        mockMvc.perform(get("/api/v1/payment/{id}", paymentId))
                .andExpect(status().isForbidden());

        verify(paymentFacade, never()).getById(any(UUID.class));
        verify(expression).isAccessPayment(eq(paymentId), any());
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void getById_WithWrongAuthority_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/payment/{id}", paymentId))
                .andExpect(status().isForbidden());

        verify(paymentFacade, never()).getById(any(UUID.class));
    }

    @Test
    void getById_Unauthenticated_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/payment/{id}", paymentId))
                .andExpect(status().isForbidden());

        verify(paymentFacade, never()).getById(any(UUID.class));
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void getById_ResourceNotFound() throws Exception {
        when(expression.isAccessPayment(eq(paymentId), any())).thenReturn(true);
        when(paymentFacade.getById(paymentId)).thenThrow(new ResourceNotFoundException("Payment not found"));

        mockMvc.perform(get("/api/v1/payment/{id}", paymentId))
                .andExpect(status().isNotFound());

        verify(paymentFacade).getById(paymentId);
        verify(expression).isAccessPayment(eq(paymentId), any());
    }

    @Test
    @WithMockUser(authorities = {"ROLE_ADMIN", "ROLE_USER"})
    void getById_WithMultipleAuthorities_Success() throws Exception {
        PaymentDto paymentDto = new PaymentDto();
        when(paymentFacade.getById(paymentId)).thenReturn(paymentDto);

        mockMvc.perform(get("/api/v1/payment/{id}", paymentId))
                .andExpect(status().isOk());

        verify(paymentFacade).getById(paymentId);
        verify(expression, never()).isAccessPayment(any(), any());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void getById_WithScopeAdmin_Forbidden() throws Exception {
        PaymentDto paymentDto = new PaymentDto();
        when(paymentFacade.getById(paymentId)).thenReturn(paymentDto);

        mockMvc.perform(get("/api/v1/payment/{id}", paymentId))
                .andExpect(status().isOk());

        verify(paymentFacade).getById(paymentId);
    }
}
