import { Component, OnInit, Output, EventEmitter } from '@angular/core';
import { FormGroup, FormControl, Validators } from '@angular/forms';
import { AuthService, RegisterRequest } from '../../services/auth.service';

@Component({
  selector: 'app-register-form',
  templateUrl: './register-form.component.html',
  styleUrls: ['./register-form.component.css']
})
export class RegisterFormComponent implements OnInit {

  registerForm!: FormGroup;
  submitted = false;
  loading = false;
  errorMessage: string | null = null;
  successMessage: string | null = null;

  // Emit form data to parent component when submitted (optional)
  @Output() formSubmitted = new EventEmitter<RegisterRequest>();

  constructor(private authService: AuthService) { }

  ngOnInit(): void {
    this.registerForm = new FormGroup({
      username: new FormControl('', [Validators.required, Validators.minLength(3)]),
      email: new FormControl('', [Validators.required, Validators.email]),
      password: new FormControl('', [Validators.required, Validators.minLength(6)]),
    });
  }

  get f(): { username: FormControl; email: FormControl; password: FormControl } {
    return this.registerForm.controls as { username: FormControl; email: FormControl; password: FormControl };
  }

  onSubmit(): void {
    this.submitted = true;
    this.errorMessage = null;
    this.successMessage = null;

    if (this.registerForm.invalid) {
      return;
    }

    this.loading = true;

    const formData: RegisterRequest = {
      username: this.f.username.value,
      email: this.f.email.value,
      password: this.f.password.value
    };

    // Call the AuthService to register the user
    this.authService.register(formData).subscribe({
      next: (msg) => {
        this.successMessage = msg; // show success message
        this.loading = false;
        this.formSubmitted.emit(formData); // emit to parent if needed
        this.registerForm.reset();
        this.submitted = false;
      },
      error: (err) => {
        this.errorMessage = err.error || 'Registration failed';
        this.loading = false;
      }
    });
  }
}
