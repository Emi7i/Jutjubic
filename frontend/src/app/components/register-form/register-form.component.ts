import { Component, OnInit, Output, EventEmitter } from '@angular/core';
import { FormGroup, FormControl, Validators, AbstractControl } from '@angular/forms';
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

  @Output() formSubmitted = new EventEmitter<RegisterRequest>();

  constructor(private authService: AuthService) { }

  ngOnInit(): void {
    this.registerForm = new FormGroup({
      username: new FormControl('', [Validators.required, Validators.minLength(3)]),
      email: new FormControl('', [Validators.required, Validators.email]),
      name: new FormControl(''),       // optional
      surname: new FormControl(''),    // optional
      address: new FormControl(''),    // optional
      password: new FormControl('', [Validators.required, Validators.minLength(6)]),
      confirmPassword: new FormControl('', [Validators.required])
    }, { validators: this.passwordsMatchValidator });
  }


  // Validator to ensure password and confirmPassword match
  passwordsMatchValidator(group: AbstractControl): { [key: string]: boolean } | null {
    const password = group.get('password')?.value;
    const confirm = group.get('confirmPassword')?.value;
    if (password && confirm && password !== confirm) {
      return { passwordsMismatch: true };
    }
    return null;
  }

  get f(): any {
    return this.registerForm.controls;
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
      name: this.f.name.value,
      surname: this.f.surname.value,
      address: this.f.address.value,
      password: this.f.password.value
    };

    this.authService.register(formData).subscribe({
      next: (msg) => {
        this.successMessage = msg;
        this.loading = false;
        this.formSubmitted.emit(formData);
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
