package isa.vezbe1.rest_example.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import isa.vezbe1.rest_example.domain.Asset;
import isa.vezbe1.rest_example.service.AssetService;

@Controller
public class AssetControllerConstructorDI {

	private AssetService assetService;

	//constructor-based dependency injection
	@Autowired
	public AssetControllerConstructorDI(AssetService assetService) {
		this.assetService = assetService;
	}


	public List<Asset> getAssets(){
		return assetService.listAssets();
	}

}
