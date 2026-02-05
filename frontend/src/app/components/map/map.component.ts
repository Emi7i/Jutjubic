import { AfterViewInit, Component, OnChanges, Input, Output, EventEmitter, SimpleChanges } from '@angular/core';
import { environment } from '../../../environments/environment';
import { Video } from '../../models/video-upload';
import * as L from 'leaflet';

@Component({
  selector: 'app-map',
  templateUrl: './map.component.html',
  styleUrls: ['./map.component.css']
})
export class MapComponent implements AfterViewInit, OnChanges {

  @Input() darkMode = false;
  @Input() videos: Video[] = [];

  @Output() markerClick = new EventEmitter<Video>();
  @Output() markerPopupClosed = new EventEmitter<Video>();
  @Output() zoomChange = new EventEmitter<number>();
  @Output() mapMoved = new EventEmitter<{ sw: [number, number], ne: [number, number] }>();

  private map!: L.Map;
  private markerLayer = L.layerGroup(); // holds all video markers
  private tileLayer!: L.TileLayer;
  private videoMarkers = new Map<string, L.Marker>(); // map video.id -> marker

  ngAfterViewInit(): void {
    // Initialize map
    this.map = L.map('map', {
      center: [44.7866, 20.4489], // default: Belgrade
      zoom: 13,
      zoomControl: false,          // remove default buttons
      attributionControl: true,    // optional: hide attribution
    });

    this.map.on("zoomend", () => {
      this.zoomChange.emit(this.map.getZoom());
      this.emitMapMoved(); // emit bounds on zoom change
    });

    this.map.on("moveend", () => {
      this.emitMapMoved(); // emit bounds on pan/zoom
    });

    this.updateMarkers();

    this.setTileLayer();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['videos'] && this.map) {
      this.updateMarkers();
    }
  }

  /** Set tile layer depending on darkMode */
  private setTileLayer(): void {
    if (this.tileLayer) this.map.removeLayer(this.tileLayer);

    const url = this.darkMode
      ? environment.mapDarkUrl
      : environment.mapLightUrl;

    const attribution = '&copy; <a href="https://www.carto.com/">CARTO</a> &copy; <a href="https://openstreetmap.org">OpenStreetMap</a>';

    const darkBg = '#101010';
    const lightBg = '#d2d2d2';

    this.map.getContainer().style.backgroundColor = this.darkMode ? darkBg : lightBg;

    this.tileLayer = L.tileLayer(url, { attribution });
    this.tileLayer.addTo(this.map);

    // add marker layer to map
    this.markerLayer.addTo(this.map);
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

  /** Emit the currently visible area to parent */
  private emitMapMoved(): void {
    const bounds = this.getVisibleArea();
    this.mapMoved.emit(bounds);
  }

  /** Close popup for a specific video */
  closePopup(videoId: string): void {
    const marker = this.videoMarkers.get(videoId);
    if (marker) {
      marker.closePopup();
    }
  }

  /** Close any open popup on the map */
  closeAnyPopup(): void {
    // Iterate over all markers and close their popups
    this.videoMarkers.forEach(marker => {
      marker.closePopup();
    });
  }

  /** Render markers based on videos */
  private updateMarkers(): void {
    const currentIds = new Set(this.videoMarkers.keys());

    // Add new markers
    this.videos.forEach(video => {
      if (!this.videoMarkers.has(video.id) && video.location) {
        const marker = L.marker([video.location.latitude, video.location.longitude])
          .bindPopup(`<b>${video.title}</b><br>${video.userName}`);

        // marker click emits video
        marker.on('click', () => {
          this.markerClick.emit(video);  // Emit the video object to parent
        });

        // marker popup close event
        marker.on('popupclose', () => this.markerPopupClosed.emit(video));

        marker.addTo(this.markerLayer);
        this.videoMarkers.set(video.id, marker);
      }
      currentIds.delete(video.id); // still present, don't remove
    });

    // Remove old markers
    currentIds.forEach(id => {
      const marker = this.videoMarkers.get(id);
      if (marker) {
        this.markerLayer.removeLayer(marker);
        this.videoMarkers.delete(id);
      }
    });
  }

}
