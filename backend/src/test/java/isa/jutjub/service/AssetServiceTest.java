package isa.jutjub.service;

import isa.jutjub.domain.Asset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
public class AssetServiceTest {

    @Test
    public void testListAssets() {
        // Arrange
        AssetServiceImpl assetService = new AssetServiceImpl();

        // Act
        List<Asset> assets = assetService.listAssets();

        // Assert
        assertNotNull(assets);
        assertEquals(2, assets.size());
        assertEquals("Asset1", assets.get(0).getName());
        assertEquals("Asset1 description", assets.get(0).getDescription());
        assertEquals("Asset2", assets.get(1).getName());
        assertEquals("Asset2 description", assets.get(1).getDescription());
    }
}
