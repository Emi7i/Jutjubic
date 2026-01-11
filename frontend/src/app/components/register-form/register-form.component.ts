import { Component, OnInit, Output, EventEmitter } from '@angular/core';
import { FormGroup, FormControl, Validators } from '@angular/forms';

@Component({
  selector: 'app-register-form',
  templateUrl: './register-form.component.html',
  styleUrls: ['./register-form.component.css']
})
export class RegisterFormComponent implements OnInit {

  registerForm!: FormGroup;
  submitted = false;

  // Emit form data to parent component when submitted
  @Output() formSubmitted = new EventEmitter<{ username: string; email: string; password: string }>();


  ngOnInit(): void {
    // Create the form controls explicitly with FormControl
    this.registerForm = new FormGroup({
      username: new FormControl('', [Validators.required, Validators.minLength(3)]),
      email: new FormControl('', [Validators.required, Validators.email]),
      password: new FormControl('', [Validators.required, Validators.minLength(6)]),
    });
  }

  // Explicitly type 'f' so we can use dot notation in the template
  get f(): { username: FormControl; email: FormControl; password: FormControl } {
    return this.registerForm.controls as { username: FormControl; email: FormControl; password: FormControl };
  }

  onSubmit(): void {
    this.submitted = true;

    if (this.registerForm.invalid) {
      return;
    }

    // Emit the form value to the parent
    this.formSubmitted.emit(this.registerForm.value);
  }
}
