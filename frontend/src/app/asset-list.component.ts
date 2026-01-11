import { Component, OnInit } from '@angular/core';
import { ApiService, Asset } from './api.service';

@Component({
  selector: 'app-asset-list',
  template: `
    <div class="container">
      <h2>Assets</h2>
      <div *ngIf="loading">Loading...</div>
      <div *ngIf="error" class="error">{{ error }}</div>
      <div *ngIf="!loading && !error">
        <div *ngIf="assets.length === 0">No assets found</div>
        <ul *ngIf="assets.length > 0">
          <li *ngFor="let asset of assets">
            {{ asset.name || 'Asset ID: ' + asset.id }}
          </li>
        </ul>
      </div>
      <button (click)="loadAssets()" class="btn-refresh">Refresh</button>
    </div>
  `,
  styles: [`
    .container {
      padding: 20px;
      max-width: 600px;
      margin: 0 auto;
    }
    .error {
      color: red;
      background: #ffebee;
      padding: 10px;
      border-radius: 4px;
      margin: 10px 0;
    }
    .btn-refresh {
      background: #007bff;
      color: white;
      border: none;
      padding: 10px 20px;
      border-radius: 4px;
      cursor: pointer;
      margin-top: 10px;
    }
    .btn-refresh:hover {
      background: #0056b3;
    }
    ul {
      list-style-type: none;
      padding: 0;
    }
    li {
      background: #f8f9fa;
      margin: 5px 0;
      padding: 10px;
      border-radius: 4px;
      border-left: 4px solid #007bff;
    }
  `]
})
export class AssetListComponent implements OnInit {
  assets: Asset[] = [];
  loading = false;
  error: string | null = null;

  constructor(private apiService: ApiService) {}

  ngOnInit(): void {
    this.loadAssets();
  }

  loadAssets(): void {
    this.loading = true;
    this.error = null;
    
    this.apiService.getAssets().subscribe({
      next: (data) => {
        this.assets = data;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Failed to load assets: ' + err.message;
        this.loading = false;
        console.error('Error loading assets:', err);
      }
    });
  }
}
