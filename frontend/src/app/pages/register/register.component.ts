import { Component } from '@angular/core';

@Component({
  selector: 'app-register',
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css']
})
export class RegisterComponent {

  handleRegister(data: { username: string, email: string, password: string }) {
    console.log('Registration data received:', data);
    // TODO: call your backend API to register the user
  }

}
