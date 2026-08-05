package com.travelplatform.busbooking.entity;

import java.util.UUID;

import java.util.Date;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "bookingdetails")
public class Bookingdetails {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	private String name;

	private String email;

	private String phoneno;

	private int age;

	private int price;

	private Date paymentdate;

	@ManyToOne
	@JoinColumn(name = "busid", referencedColumnName = "id")
	private Bus bus;
	
	@ManyToOne
	@JoinColumn(name = "userid", referencedColumnName = "id")
	private UserAdmin user;

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPhoneno() {
		return phoneno;
	}

	public void setPhoneno(String phoneno) {
		this.phoneno = phoneno;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public int getPrice() {
		return price;
	}

	public void setPrice(int price) {
		this.price = price;
	}

	public Date getPaymentdate() {
		return paymentdate;
	}

	public void setPaymentdate(Date paymentdate) {
		this.paymentdate = paymentdate;
	}

	public Bus getBus() {
		return bus;
	}

	public void setBus(Bus bus) {
		this.bus = bus;
	}

	public UserAdmin getUser() {
		return user;
	}

	public void setUser(UserAdmin user) {
		this.user = user;
	}

	@Override
	public String toString() {
		return "Bookingdetails [id=" + id + ", name=" + name + ", email=" + email + ", phoneno=" + phoneno + ", age="
				+ age + ", price=" + price + ", paymentdate=" + paymentdate + ", bus=" + bus + ", user=" + user + "]";
	}

	

	

}
