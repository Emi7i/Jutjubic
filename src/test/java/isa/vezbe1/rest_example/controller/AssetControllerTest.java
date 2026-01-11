package isa.vezbe1.rest_example.controller;

import isa.vezbe1.rest_example.domain.Asset;
import isa.vezbe1.rest_example.service.AssetService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AssetControllerTest {

    @Mock
    private AssetService assetService;

    @InjectMocks
    private AssetController assetController;

    @Test
    public void testGetAssets() {
        // Arrange
        List<Asset> expectedAssets = Arrays.asList(
            new Asset("Test Asset", "Test Description"),
            new Asset("Test Asset 2", "Test Description 2")
        );
        when(assetService.listAssets()).thenReturn(expectedAssets);

        // Act
        ResponseEntity<List<Asset>> response = assetController.getAssets();
        List<Asset> actualAssets = response.getBody();

        // Assert
        assertEquals(expectedAssets.size(), actualAssets.size());
        assertEquals(expectedAssets.get(0).getName(), actualAssets.get(0).getName());
    }
}
