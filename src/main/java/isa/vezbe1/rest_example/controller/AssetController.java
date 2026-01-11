package isa.vezbe1.rest_example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import isa.vezbe1.rest_example.domain.Asset;
import isa.vezbe1.rest_example.service.AssetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/*
 * Klasa je anotirana sa @RestController sto treba da naznaci Springu da je klasa
 * Spring Bean i da treba da bude u nadleznosti Spring kontejnera.
 * Isti efekat bi se dobio koriscenjem anotacije @Component koja je
 * nadanotacija za @RestController.
 */
@RestController
@RequestMapping("/assets")
@Tag(name = "Assets", description = "Asset management endpoints")
public class AssetController {

	//field-based dependency injection
	@Autowired
	private AssetService assetService;

	@Operation(summary = "Get all assets", description = "Returns a list of all available assets")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved assets"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
	@GetMapping
	public ResponseEntity<List<Asset>> getAssets(){
		List<Asset> assets = assetService.listAssets();
		return ResponseEntity.ok(assets);
	}
}
