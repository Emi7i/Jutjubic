import { AfterViewInit, Component, Input, Output, EventEmitter } from '@angular/core';
import * as L from 'leaflet';

@Component({
  selector: 'app-map',
  templateUrl: './map.component.html',
  styleUrls: ['./map.component.css']
})
export class MapComponent implements AfterViewInit {

  @Input() darkMode = false;

  private map!: L.Map;
  private tileLayer!: L.TileLayer;
  @Output() zoomChange = new EventEmitter<number>();

  ngAfterViewInit(): void {
    // Initialize map
    this.map = L.map('map', {
      center: [44.7866, 20.4489], // default: Belgrade
      zoom: 13,
      zoomControl: false,          // remove default buttons
      attributionControl: true,    // optional: hide attribution
    });

    this.setTileLayer();

    this.map.on("zoomend", () => {
      this.zoomChange.emit(this.map.getZoom())
    });
  }

  /** Set tile layer depending on darkMode */
  private setTileLayer(): void {
    if (this.tileLayer) this.map.removeLayer(this.tileLayer);

    const url = this.darkMode
      ? 'https://tiles.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png'
      : 'https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png';

    const attribution = '&copy; <a href="https://www.carto.com/">CARTO</a> &copy; <a href="https://openstreetmap.org">OpenStreetMap</a>';

    const darkBg = '#101010';
    const lightBg = '#d2d2d2';

    this.map.getContainer().style.backgroundColor = this.darkMode ? darkBg : lightBg;

    this.tileLayer = L.tileLayer(url, { attribution });
    this.tileLayer.addTo(this.map);
  }

  /** Zoom controls */
  zoomIn(): void {
    this.map.zoomIn();
  }

  zoomOut(): void {
    this.map.zoomOut();
  }

  getZoom(): number {
    return this.map.getZoom();
  }

  setZoom(zoom: number): void {
    this.map.setZoom(zoom);
  }

  /** Focus map on a specific location */
  focusLocation(lat: number, lng: number, zoom?: number): void {
    this.map.setView([lat, lng], zoom ?? this.map.getZoom());
  }

  /** allow toggling dark mode dynamically */
  setDarkMode(enabled: boolean): void {
    this.darkMode = enabled;
    this.setTileLayer();
  }

  /** Returns rectangle coordinates */
  getVisibleArea(): { sw: [number, number], ne: [number, number] } {
    const bounds = this.map.getBounds();
    return {
      sw: [bounds.getSouthWest().lat, bounds.getSouthWest().lng],
      ne: [bounds.getNorthEast().lat, bounds.getNorthEast().lng],
    };
  }
}
