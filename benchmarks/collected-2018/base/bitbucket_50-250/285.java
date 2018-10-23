// https://searchcode.com/api/result/102242208/

package org.timesheet.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.timesheet.domain.Employee;
import org.timesheet.domain.Manager;
import org.timesheet.domain.Task;
import org.timesheet.service.dao.EmployeeDao;
import org.timesheet.service.dao.ManagerDao;
import org.timesheet.service.dao.TaskDao;
import org.timesheet.web.editors.ManagerEditor;
import org.timesheet.web.exceptions.TaskDeleteException;

import java.util.*;

/**
 * Controller for handling Tasks.
 */
@Controller
@RequestMapping("/tasks")
public class TaskController {

    private TaskDao taskDao;
    private EmployeeDao employeeDao;
    private ManagerDao managerDao;

    @InitBinder
    protected void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(Manager.class, new ManagerEditor(managerDao));
    }

    @Autowired
    public void setTaskDao(TaskDao taskDao) {
        this.taskDao = taskDao;
    }

    @Autowired
    public void setEmployeeDao(EmployeeDao employeeDao) {
        this.employeeDao = employeeDao;
    }

    @Autowired
    public void setManagerDao(ManagerDao managerDao) {
        this.managerDao = managerDao;
    }

    public EmployeeDao getEmployeeDao() {
        return employeeDao;
    }

    public TaskDao getTaskDao() {
        return taskDao;
    }

    public ManagerDao getManagerDao() {
        return managerDao;
    }

    /**
     * Retrieves tasks, puts them in the model and returns corresponding view
     * @param model Model to put tasks to
     * @return tasks/list
     */
    @RequestMapping(method = RequestMethod.GET)
    public String showTasks(Model model) {
        model.addAttribute("tasks", taskDao.list());

        return "tasks/list";
    }

    /**
     * Deletes task with specified ID
     * @param id Task's ID
     * @return redirects to tasks if everything was ok
     * @throws TaskDeleteException When task cannot be deleted
     */
    @RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
    public String deleteTask(@PathVariable("id") long id)
            throws TaskDeleteException {

        Task toDelete = taskDao.find(id);
        boolean wasDeleted = taskDao.removeTask(toDelete);

        if (!wasDeleted) {
            throw new TaskDeleteException(toDelete);
        }

        // everything OK, see remaining tasks
        return "redirect:/tasks";
    }

    /**
     * Handles TaskDeleteException
     * @param e Thrown exception with task that couldn't be deleted
     * @return binds task to model and returns tasks/delete-error
     */
    @ExceptionHandler(TaskDeleteException.class)
    public ModelAndView handleDeleteException(TaskDeleteException e) {
        ModelMap model = new ModelMap();
        model.put("task", e.getTask());
        return new ModelAndView("tasks/delete-error", model);
    }

    /**
     * Returns task with specified ID
     * @param id Tasks's ID
     * @param model Model to put task to
     * @return tasks/view
     */
    @RequestMapping(value = "/{id}", method = RequestMethod.GET)
    public String getTask(@PathVariable("id") long id, Model model) {
        Task task = taskDao.find(id);
        model.addAttribute("task", task);

        // add all remaining employees
        List<Employee> employees = employeeDao.list();
        Set<Employee> unassignedEmployees = new HashSet<Employee>();

        for (Employee employee : employees) {
            if (!task.getAssignedEmployees().contains(employee)) {
                unassignedEmployees.add(employee);
            }
        }

        model.addAttribute("unassigned", unassignedEmployees);

        return "tasks/view";
    }

    /**
     * Removes assigned employee from task
     * @param taskId Task's ID
     * @param employeeId Assigned employee's ID
     */
    @RequestMapping(value = "/{id}/employees/{employeeId}", method = RequestMethod.DELETE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeEmployee(
            @PathVariable("id") long taskId,
            @PathVariable("employeeId") long employeeId) {

        Employee employee = employeeDao.find(employeeId);
        Task task = taskDao.find(taskId);

        task.removeEmployee(employee);
        taskDao.update(task);
    }

    /**
     * Assigns employee to tak
     * @param taskId Task's ID
     * @param employeeId Employee's ID (to assign)
     * @return redirects back to altered task: tasks/taskId
     */
    @RequestMapping(value = "/{id}/employees/{employeeId}", method = RequestMethod.PUT)
    public String addEmployee(
            @PathVariable("id") long taskId,
            @PathVariable("employeeId") long employeeId) {

        Employee employee = employeeDao.find(employeeId);
        Task task = taskDao.find(taskId);

        task.addEmployee(employee);
        taskDao.update(task);

        return "redirect:/tasks/" + taskId;
    }

    /**
     * Creates form for new task.
     * @param model Model to bind to HTML form
     * @return tasks/new
     */
    @RequestMapping(params = "new", method = RequestMethod.GET)
    public String createTaskForm(Model model) {
        model.addAttribute("task", new Task());

        // list of managers to choose from
        List<Manager> managers = managerDao.list();
        model.addAttribute("managers", managers);

        return "tasks/new";
    }

    /**
     * Saves new task to the database
     * @param task Task to save
     * @return redirects to tasks
     */
    @RequestMapping(method = RequestMethod.POST)
    public String addTask(Task task) {
        // generate employees
        List<Employee> employees = reduce(employeeDao.list());

        task.setAssignedEmployees(employees);
        taskDao.add(task);

        return "redirect:/tasks";
    }

    /**
     * Reduces list of employees to some smaller amount.
     * Simulates user interaction.
     * @param employees Employees to reduced
     * @return New list of some employees from original employees list
     */
    private List<Employee> reduce(List<Employee> employees) {
        List<Employee> reduced = new ArrayList<Employee>();
        Random random = new Random();
        int amount = random.nextInt(employees.size()) + 1;

        // max. five employees
        amount = amount > 5 ? 5 : amount;

        for (int i = 0; i < amount; i++) {
            int randomIdx = random.nextInt(employees.size());
            Employee employee = employees.get(randomIdx);
            reduced.add(employee);
            employees.remove(employee);
        }

        return reduced;
    }

}

