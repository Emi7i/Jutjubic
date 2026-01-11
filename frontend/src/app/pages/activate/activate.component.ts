import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-activate',
  templateUrl: './activate.component.html',
  styleUrls: ['./activate.component.css']
})
export class ActivateComponent implements OnInit {

  message: string | null = null;
  error: string | null = null;
  loading = true;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService
  ) { }

  ngOnInit(): void {
    // Read token from query param
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) {
      this.error = 'No activation token provided.';
      this.loading = false;
      return;
    }

    // Call activation API
    this.authService.activateAccount(token).subscribe({
      next: (msg) => {
        this.message = msg;
        this.loading = false;
        // Optionally redirect to login after a few seconds
        setTimeout(() => this.router.navigate(['/login']), 3000);
      },
      error: (err) => {
        this.error = err.error || 'Activation failed';
        this.loading = false;
      }
    });
  }

}
