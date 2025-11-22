package v1.foodDeliveryPlatform.rest;

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
import v1.foodDeliveryPlatform.exception.ResourceNotFoundException;
import v1.foodDeliveryPlatform.facade.ItemFacade;
import v1.foodDeliveryPlatform.security.SecurityUtils;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemController.class)
@Import(ControllerTestSecurityConfig.class)
@TestPropertySource(properties = {
        "restaurant.service.url=http://restaurant:8080/api/v1",
        "RESTAURANT_ENDPOINT=http://restaurant:8080/api/v1"
})
class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ItemFacade itemFacade;

    @MockitoBean
    private SecurityUtils expression;

    private final UUID itemId = UUID.randomUUID();
    private final String itemJson = """
            {
                "id": "%s",
                "dish_id": "%s",
                "quantity": 2,
                "price": 25.50
            }
            """.formatted(itemId, UUID.randomUUID());


    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void getById_WithAdminRole_Success() throws Exception {
        ItemDto itemDto = new ItemDto();
        when(itemFacade.getById(itemId)).thenReturn(itemDto);

        mockMvc.perform(get("/api/v1/items/{id}", itemId))
                .andExpect(status().isOk());

        verify(itemFacade).getById(itemId);
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void getById_WithUserRole_WithAccess_Success() throws Exception {
        ItemDto itemDto = new ItemDto();
        when(itemFacade.getById(itemId)).thenReturn(itemDto);
        when(expression.isAccessItem(eq(itemId), any())).thenReturn(true);

        mockMvc.perform(get("/api/v1/items/{id}", itemId))
                .andExpect(status().isOk());

        verify(itemFacade).getById(itemId);
        verify(expression).isAccessItem(eq(itemId), any());
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void getById_WithUserRole_NoAccess_Forbidden() throws Exception {
        when(expression.isAccessItem(eq(itemId), any())).thenReturn(false);

        mockMvc.perform(get("/api/v1/items/{id}", itemId))
                .andExpect(status().isForbidden());

        verify(itemFacade, never()).getById(any(UUID.class));
    }

    @Test
    void getById_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/items/{id}", itemId))
                .andExpect(status().isForbidden());

        verify(itemFacade, never()).getById(any(UUID.class));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void deleteById_WithAdminRole_Success() throws Exception {
        doNothing().when(itemFacade).delete(itemId);

        mockMvc.perform(delete("/api/v1/items/{id}", itemId)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(itemFacade).delete(itemId);
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void deleteById_WithUserRole_WithAccess_Success() throws Exception {
        doNothing().when(itemFacade).delete(itemId);
        when(expression.isAccessItem(eq(itemId), any())).thenReturn(true);

        mockMvc.perform(delete("/api/v1/items/{id}", itemId)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(itemFacade).delete(itemId);
        verify(expression).isAccessItem(eq(itemId), any());
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void deleteById_WithUserRole_NoAccess_Forbidden() throws Exception {
        when(expression.isAccessItem(eq(itemId), any())).thenReturn(false);

        mockMvc.perform(delete("/api/v1/items/{id}", itemId)
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(itemFacade, never()).delete(any(UUID.class));
    }

    @Test
    void deleteById_Unauthorized() throws Exception {
        mockMvc.perform(delete("/api/v1/items/{id}", itemId)
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(itemFacade, never()).delete(any(UUID.class));
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void updateItem_WithAccess_Success() throws Exception {
        ItemDto itemDto = new ItemDto();
        when(itemFacade.updateItem(any(ItemDto.class))).thenReturn(itemDto);
        when(expression.isAccessItem(eq(itemId), any())).thenReturn(true);

        mockMvc.perform(put("/api/v1/items")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemJson))
                .andExpect(status().isOk());

        verify(itemFacade).updateItem(any(ItemDto.class));
        verify(expression).isAccessItem(eq(itemId), any());
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void updateItem_NoAccess_Forbidden() throws Exception {
        when(expression.isAccessItem(eq(itemId), any())).thenReturn(false);

        mockMvc.perform(put("/api/v1/items")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemJson))
                .andExpect(status().isForbidden());

        verify(itemFacade, never()).updateItem(any(ItemDto.class));
    }

    @Test
    void updateItem_Unauthorized() throws Exception {
        mockMvc.perform(put("/api/v1/items")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(itemJson))
                .andExpect(status().isForbidden());

        verify(itemFacade, never()).updateItem(any(ItemDto.class));
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void updateItem_ValidationError() throws Exception {
        String invalidItemJson = """
                {
                    "id": "%s",
                    "dish_id": null,
                    "quantity": -1,
                    "price": -10.0
                }
                """.formatted(itemId);
        when(expression.isAccessItem(eq(itemId), any())).thenReturn(true);

        mockMvc.perform(put("/api/v1/items")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidItemJson))
                .andExpect(status().isBadRequest());

        verify(itemFacade, never()).updateItem(any(ItemDto.class));
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void getById_WithWrongAuthority_Forbidden() throws Exception {
        mockMvc.perform(get("/api/v1/items/{id}", itemId))
                .andExpect(status().isForbidden());

        verify(itemFacade, never()).getById(any(UUID.class));
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void getById_ResourceNotFound() throws Exception {
        when(expression.isAccessItem(eq(itemId), any())).thenReturn(true);
        when(itemFacade.getById(itemId)).thenThrow(new ResourceNotFoundException("Item not found"));

        mockMvc.perform(get("/api/v1/items/{id}", itemId))
                .andExpect(status().isNotFound());

        verify(itemFacade).getById(itemId);
    }
}
