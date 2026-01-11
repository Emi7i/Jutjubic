import { Component, OnInit, Output, EventEmitter } from '@angular/core';
import { FormGroup, FormControl, Validators } from '@angular/forms';
import { AuthService, LoginRequest, LoginResponse } from '../../services/auth.service';

@Component({
  selector: 'app-login-form',
  templateUrl: './login-form.component.html',
  styleUrls: ['./login-form.component.css']
})
export class LoginFormComponent implements OnInit {

  loginForm!: FormGroup;
  submitted = false;
  loading = false;
  errorMessage: string | null = null;

  // Emit the login result to parent component
  @Output() formSubmitted = new EventEmitter<LoginRequest>();
  @Output() loginSuccess = new EventEmitter<LoginResponse>();

  constructor(private authService: AuthService) { }

  ngOnInit(): void {
    this.loginForm = new FormGroup({
      usernameOrEmail: new FormControl('', [Validators.required]),
      password: new FormControl('', [Validators.required])
    });
  }

  get f(): { usernameOrEmail: FormControl; password: FormControl } {
    return this.loginForm.controls as { usernameOrEmail: FormControl; password: FormControl };
  }

  onSubmit(): void {
    this.submitted = true;
    this.errorMessage = null;

    if (this.loginForm.invalid) {
      return;
    }

    this.loading = true;

    const loginData: LoginRequest = {
      usernameOrEmail: this.f.usernameOrEmail.value,
      password: this.f.password.value
    };

    // Call AuthService to log in
    this.authService.login(loginData).subscribe({
      next: (res) => {
        this.loading = false;
        this.loginSuccess.emit(res);       // emit success to parent
        this.formSubmitted.emit(loginData); // emit login request to parent
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err.error || 'Login failed';
      }
    });
  }
}
