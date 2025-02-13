import com.supalle.Student;
import com.supalle.nice.rpc.NiceRPC;

@NiceRPC
public interface StudentService2 {

    Student<Void> getStudent(int id);

    Student<Void> getStudent2(int id);

    Student<Void> getStudent3(int id, Student<Object> student);

}
