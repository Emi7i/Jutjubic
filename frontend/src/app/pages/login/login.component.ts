import { Component } from '@angular/core';
import { LoginRequest, LoginResponse } from '../../services/auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-login-page',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent {

  constructor(private router: Router) {}

  // Handle login attempt (optional, e.g., log or analytics)
  onLoginAttempt(data: LoginRequest) {
    console.log('User attempted login:', data);
  }

  // Handle successful login
  onLoginSuccess(response: LoginResponse) {
    console.log('Login success, token:', response.token);
    localStorage.setItem('token', response.token);
    // Navigate to dashboard/home after login
    this.router.navigate(['/']);
  }
}
