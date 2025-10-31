package com.mustycodified.musty_create_order;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.util.*;

@SpringBootApplication
@Slf4j
public class MustyCreateOrderApplication {

	public static void main(String[] args) {
	 SpringApplication.run(MustyCreateOrderApplication.class, args);

		List<Integer> arrList = new ArrayList<>();
		arrList.add(1);
		arrList.add(2);
		arrList.add(3);
		arrList.add(4);
		arrList.add(5);

		Iterator<Integer> valurIterator = arrList.iterator();
		while (valurIterator.hasNext()){
			int result = valurIterator.next();
			System.out.println(result);
			if (result == 3){
				valurIterator.remove();
			}
		};
	}

	public static int singleNumber(int[] nums) {
		Set<Integer> set = new HashSet<>();
		int sumUnique =0;
		int sumAll = 0;

		for (int n:nums){
			if (!set.contains(n)){
				set.add(n);
				sumUnique+=n;
			}
			sumAll+=n;
		}
		return 2 * sumUnique -sumAll;
    }

}
