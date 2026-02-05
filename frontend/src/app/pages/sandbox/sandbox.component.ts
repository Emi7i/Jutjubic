import { AfterViewInit, Component, ViewChild } from '@angular/core';
import { MapComponent } from '../../components/map/map.component';

@Component({
  selector: 'app-sandbox',
  templateUrl: './sandbox.component.html',
  styleUrls: ['./sandbox.component.css']
})
export class SandboxComponent implements AfterViewInit {

  @ViewChild(MapComponent) mapComp!: MapComponent;

  zoom = 13; // initial zoom

  ngAfterViewInit(): void {
    // sync initial zoom from map
    this.zoom = this.mapComp.getZoom();
  }

  zoomIn(): void {
    this.mapComp.zoomIn();
    this.zoom = this.mapComp.getZoom();
  }

  zoomOut(): void {
    this.mapComp.zoomOut();
    this.zoom = this.mapComp.getZoom();
  }

  darkMode(): void {
    this.mapComp.setDarkMode(!this.mapComp.darkMode);
  }

  onSliderChange(event: any): void {
    const newZoom = Number(event.target.value);
    this.mapComp.setZoom(newZoom);
    this.zoom = newZoom;
  }

  onMapZoomChange(newZoom: number): void {
    this.zoom = newZoom;
  }
}
