import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.css']
})
export class HomeComponent {
  title = 'Jutjubic';

  constructor(private router: Router) {}

  goToVideos(): void {
    this.router.navigate(['/videos']);
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }

  goToRegister(): void {
    this.router.navigate(['/register']);
  }

  goToUpload(): void {
    this.router.navigate(['/upload']);
  }
}
